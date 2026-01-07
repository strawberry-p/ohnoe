from google import genai

f = open("app_auth")
r = f.read().split("\n")
f.close()

client = genai.Client(api_key=r[2])

def get_embarassing_message(text):
    response = client.models.generate_content(model="gemini-2.5-flash", contents="Write three completely independent derogatory, witty, and mocking sentences in first person about how you didn't manage to complete this task separated by semicolon with no extra spacing: \"$text\". If the text provided is not a humanly possible task, answer \"INCORRECT\"".replace('"$text"', text))
    return response.text

def check_image(image_bytes, task):
    response = client.models.generate_content(
      model='gemini-2.5-flash',
      contents=[
        genai.types.Part.from_bytes(
          data=image_bytes,
          mime_type='image/jpeg',
        ),
        f'''You are a visual task validator.
You will be given one image intended to show the current state of a task.

Your goal is to decide whether the task is certainly not complete, or whether the image is unrelated.

The task's name is "{task}".

Rules:

Answer NO only if the image provides clear, unambiguous evidence that the task is incomplete or incorrect.

Answer UNRELATED only if the image clearly does not depict the task at all or is irrelevant.

If completion cannot be ruled out with certainty (ambiguity, partial view, missing context), answer YES.

Do not assume missing information.

Do not explain your reasoning.

Output exactly one word.

Output format:

YES
NO
UNRELATED''']
)   
    res = response.text.upper() #type: ignore
    if res.startswith("YES"):
        return 1
    elif res.startswith("NO"):
        return 0
    else:
        return -1
