from flask import Flask, request
import threading, json
import bot

flaskApp = bot.fapp

@flaskApp.route("/add",methods=["POST"])
def task_add():
    name = request.form.get("name")
    userID = request.form.get("userID")
    ts = request.form.get("timestamp")
    print(f"request to add task {name} for {userID} at {ts}")
    success = False
    try:
        with open(bot.UPDATE_FILE,"r") as file:
            queueJson = json.load(file)
        queueJson["q"].append({"name":name,"ts":float(ts),"userID":userID}) #type: ignore
        with open(bot.UPDATE_FILE,"w") as file:
            json.dump(queueJson,file)
        success = True
    finally:
        return str(success)

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
bot.main()
