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
