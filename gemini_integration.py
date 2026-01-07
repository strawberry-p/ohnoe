from google import genai

f = open("app_auth")
r = f.read().split("\n")
f.close()

client = genai.Client(api_key=r[2])

def get_embarassing_message(text):
    response = client.models.generate_content(model="gemini-2.5-flash", contents="Write three completely independent derogatory, witty, and mocking sentences in first person about how you didn't manage to complete this task separated by semicolon with no extra spacing: \"$text\". If the text provided is not a humanly possible task, answer \"INCORRECT\"".replace('"$text"', text))
    return response.text
