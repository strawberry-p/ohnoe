import os
import dotenv, json
import datetime as dt
from slack_bolt import App
from slack_bolt.adapter.socket_mode import SocketModeHandler
import gemini_integration, x_integration
import random, threading
from flask import Flask, request
flaskApp = Flask(__name__)

def runApp():
    flaskApp.run("0.0.0.0", 8080)

threading.Thread(target=runApp).start()

dotenv.load_dotenv()
DATA_FILE = "bot-data.json"
REMINDER_SPACING = [1800,3600*2,3600*6]
REMINDER_SPACING = [30,120,300]
THREAT = [["take your Blåhaj.", "make sure you won't get any sleep tonight.", "cut all your power cables"], ["mess with your router's settings",\
          "eat your RAM", "bend your CPU's pins"], ["straighten out your clothes with a soldering iron", "spray cheap perfume all over your clothes",\
          "pop your circuit breakers"], ["log you out of all the websites you use", "make sure both sides of your pillow will be lukewarm.",\
          "underclock your CPU"]]
LAST_THREAT = "I am taking away your Blåhaj."
lastSubmitName = ""
submitCounter = 0
def dtn() -> float:
    return dt.datetime.now().timestamp()

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
    return {"name":name,"id": userID+"_"+name+"_"+str(created), "ts": ts,"userID":userID,"created":created,"iter":j,"text":text,"sent_last_reminder":False}

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
def message_alt_lock_in(message,client):
    client.chat_postEphemeral(channel=message["channel"],user=message["user"],blocks=blocks,text="Add a task. You will not regret it.")


def reminder_blocks(id,name):
    res = """[
		{
			"type": "input",
			"element": {
				"type": "radio_buttons",
				"options": [
					{
						"text": {
							"type": "plain_text",
							"text": "Yeah !",
							"emoji": true
						},
						"value": "yes"
					},
					{
						"text": {
							"type": "plain_text",
							"text": "Nope :(",
							"emoji": true
						},
						"value": "no"
					}
				],
				"action_id": "reminder_radio-action"
			},
			"label": {
				"type": "plain_text",
				"text": "Have you finished ___placeNameHere___?",
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
						"text": "Answer honestly",
						"emoji": true
					},
					"value": "___placeIDHere___",
					"action_id": "reminder_submit-action"
				}
			]
		}
	]"""
    res = res.replace("___placeIDHere___",id)
    res = res.replace("___placeNameHere___",name)
    return res

def get_threat(index: int) -> str:
    return random.choice(THREAT[index])

@app.action("submit_button-action")
def action_submit(ack, body, logger,client):
    global nextDate,nextTime,nextLabel,lastSubmitName,stored,data,submitCounter
    submitCounter += 1
    ack()
    print("submit "+str(body["state"]["values"]))
    for k in body["state"]["values"]:
        v = body["state"]["values"][k]
        get_submitted_data(v)
    
    print((nextDate,nextTime,nextLabel))
    splitDate = [int(x) for x in nextDate.split("-")]
    splitTime = [int(x) for x in nextTime.split(":")]
    obj = scheduled(nextLabel,dt.datetime(splitDate[0],splitDate[1],splitDate[2],splitTime[0],splitTime[1]).timestamp(), #type:ignore
                    dt.datetime.now().timestamp(),body["user"]["id"],text="Hello. My name is Jigsaw.")
    delta = obj["ts"]-obj["created"]
    stage = 0
    for space in REMINDER_SPACING:
        if delta > space:
            stage += 1
        else:
            break
    print(f"delta {delta} stage {stage}")
    if stage > 0:
        app.client.chat_scheduleMessage(channel=obj["userID"],
                                        text=f"Have you finished {nextLabel}",
                                        post_at=round(float(obj["ts"]))-REMINDER_SPACING[stage-1],
                                        blocks=reminder_blocks(obj["id"],nextLabel))
        data.append(obj)
        stored["data"] = data #saved into file later
    else:
        client.chat_postMessage(channel=obj["userID"],text="You should have locked in sooner. :(")
    print(body["trigger_id"])
    if nextLabel == lastSubmitName:
        print(f'deduped {body["trigger_id"]}')
    elif True:
        print("passed")
    else:
        data.append(obj)
        stored["data"] = data
        if not (body["user"]["id"] in stored["users"]):
            stored["users"].append(body["user"]["id"])
        lastSubmitName = nextLabel
    with open(DATA_FILE,"w") as file:
            try:
                #print(f"stored {stored}")
                #print(f"data {data}")
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

@app.action("reminder_radio-action")
def action_reminder_radio(ack):
    ack()

@app.action("reminder_submit-action")
def action_lazy_person(ack,body,client,say):
    ack()
    #print(body)
    print(data)
    remID = body["actions"][0]["value"]
    print(f"id {remID}")
    scheduledRes = []
    good = False
    for k in body['state']['values']:
        v = body['state']['values'][k]
        r = v.get("reminder_radio-action",-1)
        if v.get("reminder_radio-action","-1") != "-1":
            val = r["selected_option"]["value"]
            print(val)
            if r["selected_option"]["value"] == "yes" or val == "value-0":
                good = True
        else:
            print(f"{k} is not a radio value")
    objIndex = None
    j = 0
    for i in data:
        if i.get("id","-1") == remID:
            scheduledRes.append(i)
            objIndex = j
        j += 1
    print(f"res length {len(scheduledRes)}")
    if len(scheduledRes) == 0:
        obj = {}
        raise ValueError(f"ID {remID} not found")
    else:
        obj = scheduledRes[-1]
    delta = round(float(obj["ts"])-dt.datetime.now().timestamp())
    if not good:
        if delta < REMINDER_SPACING[0]:
            if obj["sent_last_reminder"]:
                say(LAST_THREAT)
            else:
                say("You lazy fuck. You procrastinated responding to a productivity bot. "+LAST_THREAT)
                if(x_integration.isReady()):
                    x_integration.post(random.choice(gemini_integration.get_embarassing_message(obj["name" \
                    ""]).split(";"))) #type: ignore
        else:
            stage = 0
            for space in REMINDER_SPACING:
                if delta+1 > space:
                    stage += 1
                else:
                    break
            if False:
                pass
            else:
                say(f"Get to work, or I will {get_threat(stage-1)}")
                client.chat_scheduleMessage(post_at=round(float(obj["ts"])-REMINDER_SPACING[stage-1]),
                                        channel=obj["userID"],blocks=reminder_blocks(obj["id"],obj["name"]),
                                        text=f"Have you finished {obj["name"]}")
            if stage == 1:
                data[objIndex]["sent_last_reminder"] = True #type: ignore
                stored["data"] = data
                with open(DATA_FILE,"w") as file:
                    json.dump(stored,file)
    else:
        say("Alright then, good job. You're safe... for now")
        print(data.pop(objIndex)) #type: ignore
        stored["data"] = data
        with open(DATA_FILE,"w") as file:
            json.dump(stored,file)
        


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
