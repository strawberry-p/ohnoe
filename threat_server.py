import random
from flask import Flask
app = Flask(__name__)
THREAT = [["just kill you.", "let you sleep with the fishes.", "make you sleep for the rest of time.", "give you the aunt treatment.", \
          "let you be aunt Bethesda.", "get you in your sleep.", "make you see your lost loved ones.", "let you have an interview with Jesus."],
          ["take your Blåhaj.", "make sure you won't get any sleep tonight.", "cut all your power cables", "mess with your router's settings",\
          "eat your RAM", "bend your CPU's pins", "oxidize your SSD's contacts", "make it impossible for you to get a job."],
          ["straighten out your clothes with a soldering iron", "spray cheap perfume all over your clothes",\
          "pop your circuit breakers", "log you out of all the websites you use", "make sure both sides of your pillow will be lukewarm tonight."]]
@app.route("/threat<index>")
def get_threat(index) -> str:
    return random.choice(THREAT[int(index)])
app.run("0.0.0.0",8081)
