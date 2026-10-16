"""Verify HTTP sessions and trip data survive a real application restart.

Requires a disposable PostgreSQL database, a built jar, JAVA_HOME/PATH, and
httpx. DATABASE_URL / DATABASE_USERNAME / DATABASE_PASSWORD configure the DB.
The script owns only its short-lived app process on loopback port 4321.
"""
import json
import os
import subprocess
import time
import uuid
from pathlib import Path

import httpx


ROOT = Path(__file__).resolve().parents[1]


def main():
    assert os.environ.get('DATABASE_URL','').startswith('jdbc:postgresql://127.0.0.1:'), 'Use disposable local PostgreSQL'
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.getenv('JAVA_HOME') else 'java'
    jar = ROOT / 'target/detour-0.1.0-SNAPSHOT.jar'
    command = [java,'-jar',str(jar),'--server.address=127.0.0.1','--server.port=4321','--debug=false','--logging.level.root=ERROR']
    def start():
        process = subprocess.Popen(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            if process.poll() is not None: raise RuntimeError('Test app failed to start')
            try:
                if httpx.get('http://127.0.0.1:4321/actuator/health', timeout=2).status_code == 200: return process
            except httpx.HTTPError: pass
            time.sleep(.5)
        process.terminate()
        raise TimeoutError('Test app startup')
    def stop(process):
        process.terminate()
        try: process.wait(timeout=30)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
    process = start()
    try:
        with httpx.Client(base_url='http://127.0.0.1:4321',timeout=20) as client:
            def token():
                csrf = client.get('/api/auth/csrf').json()
                return {csrf['headerName']:csrf['token']}
            identity = uuid.uuid4().hex
            response = client.post('/api/auth/register', headers=token(), json={'displayName':'Persistence QA','email':identity+'@example.com','password':uuid.uuid4().hex})
            response.raise_for_status()
            response = client.post('/api/trips', headers=token(), json={'name':'Persistent QA trip','destination':'Seoul','startDate':'2026-10-17','endDate':'2026-10-18','currency':'USD'})
            response.raise_for_status()
            trip_id = response.json()['id']
            stop(process)
            process = start()
            session = client.get('/api/auth/me').json()
            assert session['authenticated'] is True
            response = client.get('/api/trips/' + trip_id)
            response.raise_for_status()
            assert response.json()['name'] == 'Persistent QA trip'
            print(json.dumps({'postgresql':'passed','application_restart':'passed','persisted_session':'passed','persisted_trip':'passed'}))
    finally:
        if process.poll() is None: stop(process)


if __name__ == '__main__': main()
