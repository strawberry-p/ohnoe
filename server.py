from flask import Flask, request
import threading
import bot
flaskApp = Flask(__name__)
def runApp():
    flaskApp.run("0.0.0.0", 8080)
botThread = threading.Thread(target=bot.main)
flaskThread = threading.Thread(target=runApp)
flaskThread.start()

@flaskApp.route("/add",methods=["POST"])
def task_add():
    name = request.form.get("name")
    userID = request.form.get("userID")
    ts = request.form.get("timestamp")
    return bot.add_task(name,float(ts),userID) #type: ignore

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
