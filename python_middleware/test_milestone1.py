import unittest
import asyncio
import httpx
from main import app, verify_firebase_token, build_system_instruction, SECRET_LORE_GUIDELINES

class TestMilestone1(unittest.TestCase):
    def run_async(self, coro):
        return asyncio.run(coro)

    def test_root_endpoint(self):
        async def _test():
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
                response = await client.get("/")
                self.assertEqual(response.status_code, 200)
                self.assertEqual(response.json(), {"status": "ok", "message": "RPG Game Middleware is running"})
        self.run_async(_test())

    def test_unauthenticated_action_returns_401(self):
        async def _test():
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
                response = await client.post("/action", json={
                    "session_id": "test_sess",
                    "player_id": "player1",
                    "action": "olhar redor"
                })
                self.assertEqual(response.status_code, 401)
                self.assertIn("detail", response.json())
        self.run_async(_test())

    def test_unauthenticated_generate_image_returns_401(self):
        async def _test():
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
                response = await client.post("/generate-image", json={
                    "session_id": "test_sess",
                    "prompt_description": "A dark tavern"
                })
                self.assertEqual(response.status_code, 401)
                self.assertIn("detail", response.json())
        self.run_async(_test())

    def test_invalid_bearer_token_returns_401(self):
        async def _test():
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
                headers = {"Authorization": "Bearer invalid_token_xyz"}
                response = await client.post("/action", headers=headers, json={
                    "session_id": "test_sess",
                    "player_id": "player1",
                    "action": "olhar redor"
                })
                self.assertEqual(response.status_code, 401)
        self.run_async(_test())

    def test_secret_lore_system_instruction(self):
        instruction = build_system_instruction()
        self.assertIn("DIRETRIZES DE LORE SECRETO DO MUNDO", instruction)
        self.assertIn("Culto das Sombras", instruction)
        self.assertIn("Garrick", instruction)
        self.assertIn("NUNCA revele segredos", instruction)

    def test_authenticated_generate_image_fallback(self):
        async def _test():
            app.dependency_overrides[verify_firebase_token] = lambda: {"uid": "test_user_123"}
            try:
                async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
                    response = await client.post("/generate-image", json={
                        "session_id": "test_sess_001",
                        "prompt_description": "A glowing mystical sword"
                    })
                    self.assertEqual(response.status_code, 200)
                    data = response.json()
                    self.assertEqual(data["status"], "success")
                    self.assertIn("image_url", data)
                    self.assertIn(data["is_mock"], [True, False])
            finally:
                app.dependency_overrides.clear()
        self.run_async(_test())

if __name__ == "__main__":
    unittest.main()
