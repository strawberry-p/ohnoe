from xdk.oauth2_auth import OAuth2PKCEAuth
import webbrowser
from xdk import Client
import threading, os
from flask import Flask, request

app = Flask(__name__)

f = open("app_auth")
r = f.read().split("\n")
f.close()
auth = OAuth2PKCEAuth(
    client_id=r[0],
    client_secret=r[1],
    redirect_uri="http://localhost:8080/callback",
    scope="tweet.read users.read offline.access tweet.write"
)

client = None
ready = False

app = Flask(__name__)

def init(a):
    global app
    app = a

@app.route("/callback")
def callback():
    global client, ready
    code = request.args.get("code")
    tokens = auth.fetch_token(authorization_response=f'http://localhost:8080/callback?code={code}')
    access_token = tokens["access_token"]
    refresh_token = tokens["refresh_token"]  # Store for renewal
    client = Client(bearer_token=access_token)
    client = Client(token=tokens)
    ready = True
    f = open("auth_stuff", 'w')
    buffer = ""
    for i in tokens:
        buffer +=f"{i}:: {tokens[i]}\n"
    f.write(buffer.removesuffix("\n"))
    return "Hell yeah"
@app.route("/post", methods=["POST"])
def r_post():
    text = request.form.get("text")
    post(text)
    return "Hell yeah"
@app.route("/auth")
def authf():
    auth_url = auth.get_authorization_url()
    print(f"Visit this URL to authorize: {auth_url}")
    webbrowser.open(auth_url)
@app.route("/ready")
def readyf():
    global ready
    if ready:
        return '1'
    else:
        return '0'

if os.path.exists("auth_stuff"):
    f = open("auth_stuff")
    r = f.read().split("\n")
    tokens = {}
    for i in r:
        try:
            tokens[i.split(":: ")[0]] = float(i.split(":: ")[1])
        except:
            tokens[i.split(":: ")[0]] = i.split(":: ")[1]
    try:
        client = Client(token=tokens)
    except:
        auth = OAuth2PKCEAuth(
            client_id=r[0],
            client_secret=r[1],
            redirect_uri="http://localhost:8080/callback",
            scope="tweet.read users.read offline.access tweet.write",
            token=tokens
        )
        tokens = auth.refresh_token()
        client = Client(bearer_token=tokens["access_token"])
        client = Client(token=tokens)
    ready = True

def post(text):
    global client
    client.posts.create({"text": text})

def isReady():
    global ready
    return ready