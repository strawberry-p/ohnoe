# Ohnoe
> REAL motivation for everyday tasks

## Run
### Start
```sh
git clone git@github.com:strawberry-p/ohnoe.git
cd ohnoe
```

### Server (on the same machine as desktop)
> on Windows use set instead of export
```sh
source venv/bin/activate
export SLACK_BOT_TOKEN="<your-slack-bot-token>"
python server.py
```

### Desktop
> on Windows use set instead of export
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
