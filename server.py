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
        bot.app.client.chat_postMessage(channel=MSG_UPDATE_CHANNEL,text=f"adding task: {name}")
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
