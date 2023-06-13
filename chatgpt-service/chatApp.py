from flask import Flask, request, jsonify
import openai

app = Flask(__name__)

openai.api_key = "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"


@app.route('/chat', methods=['POST'])
def chat():
    content = request.form.get('content')
    completion = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        temperature=0.8,
        max_tokens=2000,
        messages=[{"role": "system", "content": "You are a poet who creates poems that evoke emotions."},
                  {"role": "user", "content": content}],
    )
    result = completion.choices[0].message.content
    print(result)
    return result


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8082)
