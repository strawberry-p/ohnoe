import os
import dotenv, json
import datetime as dt
from slack_bolt import App
from slack_bolt.adapter.socket_mode import SocketModeHandler
dotenv.load_dotenv()
DATA_FILE = "bot-data.json"
lastSubmitName = ""
submitCounter = 0
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
    def __dict__(self):
        return {"name":self.name,"id": self.id, "ts": self.ts,"userID":self.userID,"created":self.created,"iter":self.iter,"text":self.text}
        

def scheduled(name,ts,created,userID,j=0,text=""):
    return {"name":name,"id": userID+"_"+name+"_"+str(created), "ts": ts,"userID":userID,"created":created,"iter":j,"text":text}

def get_submitted_data(inp: dict):
    global nextDate,nextTime,nextLabel
    if inp.get("datepicker-action","-1") != "-1":
        nextDate = inp["datepicker-action"]["selected_date"]
    if inp.get("timepicker-action","-1") != "-1":
        nextTime = inp["timepicker-action"]["selected_time"]
    if inp.get("task_label_input-action",-1) != -1: #avoiding intended string
        nextLabel = inp["task_label_input-action"]["value"]

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
    global nextDate,nextTime,nextLabel,lastSubmitName,stored,data,submitCounter
    submitCounter += 1
    ack()
    print("submit "+str(body["state"]["values"]))
    print(body.get('value',"no value in submit button"))
    for k in body["state"]["values"]:
        v = body["state"]["values"][k]
        get_submitted_data(v)
    
    print((nextDate,nextTime,nextLabel))
    splitDate = [int(x) for x in nextDate.split("-")]
    splitTime = [int(x) for x in nextTime.split(":")]
    obj = scheduled(nextLabel,dt.datetime(splitDate[0],splitDate[1],splitDate[2],splitTime[0],splitTime[1]).timestamp(), #type:ignore
                    dt.datetime.now().timestamp(),body["user"]["id"],text="Hello. My name is Jigsaw.")
    print(f"body length {len(str(body))}")
    print(body["trigger_id"])
    if nextLabel == lastSubmitName:
        print(f'deduped {body["trigger_id"]}')
    else:
        data.append(obj)
        stored["data"] = data
        if not (body["user"]["id"] in stored["users"]):
            stored["users"].append(body["user"]["id"])
        lastSubmitName = nextLabel
    with open(DATA_FILE,"w") as file:
            try:
                print(f"stored {stored}")
                print(f"data {data}")
                json.dump(stored,file)
            except Exception as _:
                print(_)
                print(stored)
                print(data)
    print(f"counter {submitCounter}")
    nextDate,nextTime,nextLabel = (0,0,0)
    splitDate = []
    splitTime = []
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
    try:
        SocketModeHandler(app, os.environ["SLACK_APP_TOKEN"]).start()
    except KeyboardInterrupt:
        print("interrupted")
        print(data)
        with open(DATA_FILE,"w") as file:
            json.dump(stored,file)
    except Exception as _:
        print(f"exiting, {_}")
        print(data)
        with open(DATA_FILE,"w") as file:
            json.dump(stored,file)
