import os
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials, db

load_dotenv()
try:
    cred = credentials.Certificate("serviceAccountKey.json")
    database_url = os.environ.get("DATABASE_URL")
    firebase_admin.initialize_app(cred, {'databaseURL': database_url})
    ref = db.reference("sessions/test")
    print("GET:", ref.get())
    print("SUCCESS")
except Exception as e:
    print(f"ERROR: {type(e).__name__} - {str(e)}")
