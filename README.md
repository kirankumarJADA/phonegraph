# PhoneGraph: Knowledge-Graph-Grounded Smartphone Recommender 

PhoneGraph is a novel smartphone recommendation system developed as my MSc dissertation project at Birmingham City University. It combines a domain-specific knowledge graph with a large language model (GLM-5.2) to generate personalized, natural language phone recommendations.

## Key Features

- Ingests a custom knowledge graph of over 2,000 smartphone models and their attributes
- Uses pgvector for efficient semantic search over phone embeddings
- Feeds top graph matches into GLM-5.2 to produce fluent, comparative recommendations
- Applies heuristic checks to quantify hallucination in generated text
- Exposes RESTful microservices for searching, recommending, and model orchestration

## Architecture

PhoneGraph is implemented as a suite of Java microservices:

- **Catalog Service**: Manages the core smartphone model data
- **Knowledge Graph Service**: Loads the phone graph into Neo4j and wraps graph queries
- **Semantic Search Service**: Indexes phone attributes in pgvector for fast embeddings search
- **LLM Orchestrator**: Calls GLM-5.2 via NVIDIA's Inference API with KG-conditioned prompts
- **Evaluation Service**: Runs benchmark phone queries and scores generated recs

Microservices communicate via RESTful APIs. The KG and Semantic Search services use Neo4j and Postgres/pgvector respectively for storage.

## Dataset

The PhoneGraph dataset includes 2000 smartphone models released between 2015-2024 with attributes like brand, price, chipset, camera specs, and more. Compiled from public sources like GSMArena and PhoneArena.

## Progress

- [x] Compiled knowledge graph of 2000 phone models 
- [x] Implemented KG microservice to load graph into Neo4j
- [x] Added semantic search service using pgvector embeddings
- [x] Integrated GLM-5.2 via NVIDIA Inference API for text generation
- [x] Developed heuristic hallucination detection based on KG
- [ ] Full evaluation on benchmark phone rec queries
- [ ] Experiments with other LLMs and prompt variations
- [ ] Web UI for interactive recommendations

## About Me

I'm Kiran Kumar Jada, an MSc Advanced Computer Science student at Birmingham City University. PhoneGraph is my dissertation project to explore applications of knowledge graphs and large language models in recommender systems. Feel free to reach out if you're working on similar research!
