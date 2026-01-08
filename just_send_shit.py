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
