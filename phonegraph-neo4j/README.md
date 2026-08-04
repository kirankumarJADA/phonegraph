# PhoneGraph — Neo4j Setup

## What's in this folder

| File | Purpose |
|---|---|
| docker-compose.yml | Runs Neo4j in Docker |
| load_phones.py | Loads 2,000 phones into Neo4j |
| cypher/01_schema.cypher | Creates constraints and indexes |
| cypher/02_sample_queries.cypher | Test queries after loading |

---

## Step-by-step setup

### Step 1 — Install Docker
Download from https://www.docker.com/products/docker-desktop

### Step 2 — Copy your CSV here
Put smartphone_specs_dataset_2015_2024.csv in this same folder

### Step 3 — Start Neo4j
Open terminal in this folder and run:
```
docker-compose up -d
```
Wait 30 seconds for Neo4j to start.

### Step 4 — Install Python dependencies
```
pip install neo4j pandas
```

### Step 5 — Load the data
```
python load_phones.py
```
This loads all 2,000 phones with all 9 entities.
Takes about 5-10 minutes.

### Step 6 — Open Neo4j Browser
Go to: http://localhost:7474
- Username: neo4j
- Password: phonegraph2024

### Step 7 — Test it works
Run this in Neo4j Browser:
```
MATCH (p:Phone) RETURN count(p)
```
Should return 2000.

---

## The 9 entities and relationships

```
(Phone)-[:MADE_BY]-------->(Brand)
(Phone)-[:RUNS]------------>(OS)
(Phone)-[:HAS_CHIPSET]---->(Chipset)
(Phone)-[:HAS_DISPLAY]---->(Display)
(Phone)-[:HAS_BATTERY]---->(Battery)
(Phone)-[:HAS_CAMERA]----->(Camera)
(Phone)-[:SUPPORTS_FEATURE]->(Feature)
(Phone)-[:HAS_VARIANT]---->(StorageVariant)
```

---

## Connection details for Spring Boot
```
spring.neo4j.uri=bolt://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=phonegraph2024
```
