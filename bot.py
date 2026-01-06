import os
import dotenv, json
import datetime as dt
from slack_bolt import App
from slack_bolt.adapter.socket_mode import SocketModeHandler
dotenv.load_dotenv()
try:
    with open("bot-data.json") as file:
        stored = json.load(file)
except Exception as _:
    print(_)
    stored = {"users":[],"data":[]}
    with open("bot-data.json","x") as file:
        json.dump(stored,file)
data = stored["data"]
nextDate = ""
nextTime = ""
nextLabel = ""
class Scheduled:
    def __init__(self,name,ts,created,userID,j=0,text=""):
        self.id = userID+"_"+name+"_"+str(created)
        self.ts = ts
        self.userID = userID
        self.name = name
        self.created = created
        self.iter = j
        self.text = text
    def until(self):
        return self.ts-dt.datetime.now().timestamp
        
# Initializes your app with your bot token and socket mode handler
app = App(token=os.environ.get("SLACK_BOT_TOKEN"),signing_secret=os.environ.get("SLACK_SIGNING_SECRET"))
print(os.environ.get("SLACK_BOT_TOKEN")[:10]) #type: ignore
blocks = json.loads("""{
	"blocks": [
		{
			"type": "section",
			"text": {
				"type": "plain_text",
				"text": "that is true. elaborate.",
				"emoji": true
			}
		},
		{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "When's the deadline?"
			},
			"accessory": {
				"type": "datepicker",
				"initial_date": "2026-01-07",
				"placeholder": {
					"type": "plain_text",
					"text": "Select a date",
					"emoji": true
				},
				"action_id": "datepicker-action"
			}
		},
		{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "What time?"
			},
			"accessory": {
				"type": "timepicker",
				"initial_time": "23:59",
				"placeholder": {
					"type": "plain_text",
					"text": "Select time",
					"emoji": true
				},
				"action_id": "timepicker-action"
			}
		},
		{
			"type": "input",
			"element": {
				"type": "plain_text_input",
				"action_id": "task_label_input-action"
			},
			"label": {
				"type": "plain_text",
				"text": "Name",
				"emoji": true
			},
			"optional": false
		},
		{
			"type": "actions",
			"elements": [
				{
					"type": "button",
					"text": {
						"type": "plain_text",
						"text": "Add task",
						"emoji": true
					},
					"value": "click_me_123",
					"action_id": "submit_button-action"
				}
			]
		}
	]
}""")["blocks"]

@app.message("lockin")
def message_lockin(message,client):
    print("received "+str(message))
    client.chat_postEphemeral(channel=message["channel"],user=message["user"],blocks=blocks,text="Add a task. You will not regret it.")

@app.message("lock in")
def message_alt_lock_in(message,say):
    say(blocks=blocks,text="Add a task. You will not regret it.")



@app.action("submit_button-action")
def action_submit(ack, body, logger):
    ack()
    print("submit "+str(body["state"]["values"]))
    logger.info(body)

@app.action("datepicker-action")
def action_datepicker(ack, body, logger):
    ack()
    logger.info(body)

@app.action("timepicker-action")
def action_timepicker(ack, body, logger):
    ack()
    logger.info(body)

@app.action("task_label_input-action")
def action_tasklabel(ack,body,logger):
    ack()
    logger.info(body)


if __name__ == "__main__":
    #app.start(port=int(os.environ.get("PORT", 3000)))
    SocketModeHandler(app, os.environ["SLACK_APP_TOKEN"]).start()
