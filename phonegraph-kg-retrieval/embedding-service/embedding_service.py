"""
PhoneGraph — Embedding Microservice
=====================================
A tiny Flask service that wraps the same all-MiniLM-L6-v2 model used in
Step 3 (setup_pgvector.py). The Java KG & Retrieval Service calls this
over HTTP to turn a user's query text into the same 384-number vector
format stored in pgvector, so query and data live in the same "meaning
space" and can be compared.

Why a separate Python service instead of doing this in Java directly?
  - The embedding model (sentence-transformers) is a Python library.
  - Keeping it as its own tiny service means the Java backend doesn't
    need a Python runtime bundled in — it just makes an HTTP call.
  - This also mirrors real-world microservice patterns: one small,
    focused service per responsibility.

USAGE:
    pip install flask sentence-transformers
    python embedding_service.py
    (runs on http://localhost:5001)
"""

from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer

app = Flask(__name__)

print("Loading embedding model (all-MiniLM-L6-v2)...")
model = SentenceTransformer("all-MiniLM-L6-v2")
print("Model ready. Embedding service listening on port 5001.")


@app.route("/embed", methods=["POST"])
def embed():
    data = request.get_json()
    text = data.get("text", "")
    if not text:
        return jsonify({"error": "No text provided"}), 400

    vector = model.encode([text])[0].tolist()
    return jsonify({"embedding": vector, "dimensions": len(vector)})


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": "all-MiniLM-L6-v2"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
