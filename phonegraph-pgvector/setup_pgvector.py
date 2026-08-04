import psycopg2
import pandas as pd
from sentence_transformers import SentenceTransformer

DB_CONFIG = {
    "host":     "127.0.0.1",
    "port":     5432,
    "dbname":   "phonegraph",
    "user":     "phonegraph",
    "password": "phonegraph2024"
}

CSV_FILE = r"C:\Users\jadak\OneDrive\Desktop\msc disseation project\phonegraph-neo4j\smartphone_specs_dataset_2015_2024.csv"
MODEL_NAME = "all-MiniLM-L6-v2"

def setup_database(conn):
    print("Setting up pgvector...")
    cur = conn.cursor()
    cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
    cur.execute("DROP TABLE IF EXISTS phone_embeddings;")
    cur.execute("""
        CREATE TABLE phone_embeddings (
            id          SERIAL PRIMARY KEY,
            phone_name  TEXT,
            brand       TEXT,
            year        TEXT,
            os          TEXT,
            chipset     TEXT,
            price       INTEGER,
            ram         TEXT,
            storage     TEXT,
            display     TEXT,
            battery     TEXT,
            camera      TEXT,
            nfc         TEXT,
            five_g      TEXT,
            description TEXT,
            embedding   vector(384)
        );
    """)
    conn.commit()
    cur.close()
    print("Database ready!")

def make_description(row):
    return (
        f"{row.get('Phone Name','')} is a {row.get('Year Released','')} smartphone by {row.get('Brand','')}. "
        f"Runs {row.get('OS','')} with {row.get('Chipset','')} chipset. "
        f"{row.get('Display Size (inches)','')} inch {row.get('Display Type','')} display. "
        f"Camera: {row.get('Main Camera','')} rear, {row.get('Front Camera','')} front. "
        f"Battery: {row.get('Battery (mAh)','')}mAh, {row.get('Charging','')} charging. "
        f"RAM: {row.get('RAM (GB)','')}GB, Storage: {row.get('Storage (GB)','')}GB. "
        f"NFC: {row.get('NFC','')}, 5G: {row.get('5G','')}, Price: ${row.get('Price (USD)','')}"
    )

def load_embeddings(conn):
    print(f"Loading model {MODEL_NAME}...")
    print("First time download is 90MB, please wait...")
    model = SentenceTransformer(MODEL_NAME)
    print("Model loaded!")
    df = pd.read_csv(CSV_FILE)
    print(f"Generating embeddings for {len(df)} phones...")
    cur = conn.cursor()
    batch = []
    success = 0
    for i, row in df.iterrows():
        desc = make_description(row)
        try:
            price = int(str(row.get("Price (USD)","0")).replace(",",""))
        except:
            price = 0
        batch.append({
            "phone_name": str(row.get("Phone Name",""))[:200],
            "brand":      str(row.get("Brand","")),
            "year":       str(row.get("Year Released","")),
            "os":         str(row.get("OS","")),
            "chipset":    str(row.get("Chipset","")),
            "price":      price,
            "ram":        str(row.get("RAM (GB)","")),
            "storage":    str(row.get("Storage (GB)","")),
            "display":    str(row.get("Display Type","")),
            "battery":    str(row.get("Battery (mAh)","")),
            "camera":     str(row.get("Main Camera","")),
            "nfc":        str(row.get("NFC","")),
            "five_g":     str(row.get("5G","")),
            "description": desc,
        })
        if len(batch) >= 50 or i == len(df) - 1:
            descriptions = [b["description"] for b in batch]
            embeddings = model.encode(descriptions, show_progress_bar=False)
            for item, emb in zip(batch, embeddings):
                cur.execute("""
                    INSERT INTO phone_embeddings
                    (phone_name, brand, year, os, chipset, price, ram, storage,
                     display, battery, camera, nfc, five_g, description, embedding)
                    VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
                """, (
                    item["phone_name"], item["brand"], item["year"],
                    item["os"], item["chipset"], item["price"],
                    item["ram"], item["storage"], item["display"],
                    item["battery"], item["camera"], item["nfc"],
                    item["five_g"], item["description"],
                    emb.tolist()
                ))
                success += 1
            conn.commit()
            batch = []
            print(f"  Embedded {success}/{len(df)} phones...")
    cur.close()
    print(f"Done! {success} embeddings stored!")

def test_search(conn):
    print("\nTesting semantic search...")
    model = SentenceTransformer(MODEL_NAME)
    queries = ["best camera phone under 500", "Samsung flagship 2023", "cheap phone big battery"]
    cur = conn.cursor()
    for query in queries:
        emb = model.encode([query])[0].tolist()
        cur.execute("""
            SELECT phone_name, brand, price,
                   1 - (embedding <=> %s::vector) AS similarity
            FROM phone_embeddings
            ORDER BY embedding <=> %s::vector
            LIMIT 3
        """, (emb, emb))
        results = cur.fetchall()
        print(f"\nQuery: '{query}'")
        for r in results:
            print(f"  -> {r[0]} ({r[1]}) ${r[2]}")
    cur.close()

if __name__ == "__main__":
    print("PhoneGraph pgvector Setup")
    print("="*50)
    conn = psycopg2.connect(**DB_CONFIG)
    print("Connected!")
    setup_database(conn)
    load_embeddings(conn)
    test_search(conn)
    conn.close()
    print("\nAll done! pgvector ready!")