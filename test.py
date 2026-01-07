import dotenv, os
from slack_bolt import App
from slack_bolt.adapter.socket_mode import SocketModeHandler
dotenv.load_dotenv()
app = App(token=os.environ.get("SLACK_BOT_TOKEN"),signing_secret=os.environ.get("SLACK_SIGNING_SECRET"))
app.client.chat_postMessage(channel="U0A6FHCNS15",text="hello there")