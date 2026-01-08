# Ohnoe
> REAL motivation for everyday tasks

## Run
### Start
```
git clone git@github.com:strawberry-p/ohnoe.git
cd ohnoe
```

### Server (on the same machine as desktop)
both
```sh
source venv/bin/activate
export SLACK_BOT_TOKEN="<your-slack-bot-token>"
```
then
```sh
python just_send_shit.py 
```
and
```sh
python server.py
```

### Desktop
```
export BLUESKY_USERNAME="<your-bluesky-handle>"
export BLUESKY_PASSWORD="<your-bluesky-password>"
export GEMINI_TOKEN="<gemini-api-token>"
```
```sh
./gradlew run
```
or
```sh
./gradlew packageUberJarForCurrentOS && java -jar ohnoe-all.jar
```

### Integrations ("API" button)
- Twitter (X) - press the button, and then press "Authorize" in the opened browser window
- Bluesky - press the button, wait a bit until activation (will work from environment credentials) 
