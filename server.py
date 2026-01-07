from flask import Flask, request
import threading
import bot, gemini_integration
import x_integration
flaskApp = Flask(__name__)
def runApp():
    flaskApp.run("0.0.0.0", 8080)

threading.Thread(target=runApp).start()