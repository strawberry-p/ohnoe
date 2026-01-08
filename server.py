from flask import Flask, request
import threading, json
import bot
MSG_UPDATE_CHANNEL = "C0A6FHD4JKZ"
flaskApp = bot.fapp

def up_first(inp: str) -> str:
    return inp[:1].upper()+inp[1:]

@flaskApp.route("/add",methods=["POST"])
def task_add():
    name = request.form.get("name")
    userID = request.form.get("userID")
    ts = request.form.get("timestamp")
    delete = up_first(request.form.get("delete","False"))
    if delete == "True":
        deleteBool = True
    else:
        deleteBool = False
    print(f"request to add ({deleteBool}) task {name} for {userID} at {ts}")
    success = False
    try:
        with open(bot.UPDATE_FILE,"r") as file:
            queueJson = json.load(file)
        queueJson["q"].append({"name":name,"ts":float(ts),"userID":userID,"delete":deleteBool}) #type: ignore
        with open(bot.UPDATE_FILE,"w") as file:
            json.dump(queueJson,file)
        bot.app.client.chat_postMessage(channel=MSG_UPDATE_CHANNEL,text=f"updating task: {name}")
        success = True
    finally:
        return str({"ok":success,"id":str(userID)+"_"+str(name)+"_"+str(ts)})

@flaskApp.route("/is_done", methods=["POST"])
def is_done():
    task = request.form.get("task")
    image = request.files.get("image.jpg")
    image_bytes = image.read()
    match bot.gemini_integration.check_image(image_bytes, task):
        case -1:
            return 'Unrelated image.'
        case 0:
            return 'Task not done.'
        case 1:
            return 'Task done!'
        case _:
            return 'Unrelated image.'

@flaskApp.route("/threat<tier>",methods=["GET"])
def send_thread(tier):
    return bot.get_threat(int(tier)) #type: ignore

def runApp():
    flaskApp.run("0.0.0.0", 8080)
threading.Thread(target=runApp).start()
bot.main()


import os
import random
from flask import Flask, request, jsonify
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError

app = Flask(__name__)

slack_client = WebClient(token=os.environ.get("SLACK_BOT_TOKEN"))

THREAT = [["just kill you.", "let you sleep with the fishes.", "make you sleep for the rest of time.", "give you the aunt treatment.", \
          "let you be aunt Bethesda.", "get you in your sleep.", "make you see your lost loved ones.", "let you have an interview with Jesus."],
          ["take your Blåhaj.", "make sure you won't get any sleep tonight.", "cut all your power cables", "mess with your router's settings",\
          "eat your RAM", "bend your CPU's pins", "oxidize your SSD's contacts", "make it impossible for you to get a job."],
          ["straighten out your clothes with a soldering iron", "spray cheap perfume all over your clothes",\
          "pop your circuit breakers", "log you out of all the websites you use", "make sure both sides of your pillow will be lukewarm tonight."]]


@app.route("/send-slack", methods=["POST"])
def send_to_slack():
    data = request.json
    channel_id = data.get("channel")
    index = int(data.get("index")) 
    message_text = random.choice(THREAT[index]) 

    if not channel_id or not message_text:
        return jsonify({"error": "Missing channel or text"}), 400

    try:
        response = slack_client.chat_postMessage(
            channel=channel_id,
            text="I will " + message_text
        )
        return jsonify({"status": "success", "ts": response["ts"]}), 200

    except SlackApiError as e:
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == "__main__":
    app.run(port=5000)

