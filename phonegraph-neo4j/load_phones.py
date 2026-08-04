"""
PhoneGraph — Neo4j Data Loader
===============================
Loads smartphone_specs_dataset_2015_2024.csv into Neo4j
with all 9 entities and relationships.

REQUIREMENTS:
    pip install neo4j pandas

USAGE:
    1. Start Neo4j:  docker-compose up -d
    2. Wait 30 seconds for Neo4j to start
    3. Run:          python load_phones.py
    4. Open browser: http://localhost:7474
       Username: neo4j  |  Password: phonegraph2024
"""

from neo4j import GraphDatabase
import pandas as pd
import re
import time

# ── CONFIG ──
NEO4J_URI      = "bolt://localhost:7687"
NEO4J_USER     = "neo4j"
NEO4J_PASSWORD = "phonegraph2024"
CSV_FILE       = "smartphone_specs_dataset_2015_2024.csv"  # put CSV in same folder

# ── FEATURES TO DETECT ──
FEATURE_RULES = {
    "NFC":            lambda r: str(r.get("NFC","")).strip() == "Yes",
    "5G":             lambda r: str(r.get("5G","")).strip() == "Yes",
    "4G LTE":         lambda r: str(r.get("5G","")).strip() != "Yes",
    "Wireless Charging": lambda r: "wireless" in str(r.get("Charging","")).lower(),
    "MagSafe":        lambda r: "magsafe" in str(r.get("Charging","")).lower(),
    "USB-C":          lambda r: "USB-C" in str(r.get("USB Type","")),
    "Lightning":      lambda r: "Lightning" in str(r.get("USB Type","")),
}

def clean(val, default="Unknown"):
    if val is None or str(val).strip() in ["","nan","None","N/A"]:
        return default
    return str(val).strip()

def extract_dxo(charging_str):
    """DXOMark not in CSV — return N/A, will be enriched later"""
    return "N/A"

def extract_wired_charging(charging_str):
    s = str(charging_str)
    m = re.search(r'(\d+)W', s)
    return m.group(1)+"W" if m else "Unknown"

def extract_wireless(charging_str):
    s = str(charging_str).lower()
    if "wireless" in s or "magsafe" in s or "qi" in s:
        m = re.search(r'(\d+)w\s*wireless|magsafe\s*(\d+)', s)
        if m:
            val = m.group(1) or m.group(2)
            return val+"W"
        return "Yes"
    return "No"

class PhoneGraphLoader:
    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        print("Connected to Neo4j!")

    def close(self):
        self.driver.close()

    def run(self, query, **params):
        with self.driver.session() as session:
            return session.run(query, **params)

    def clear_db(self):
        print("Clearing existing data...")
        self.run("MATCH (n) DETACH DELETE n")
        print("Database cleared.")

    def create_schema(self):
        print("Creating constraints and indexes...")
        constraints = [
            "CREATE CONSTRAINT phone_name IF NOT EXISTS FOR (p:Phone) REQUIRE p.name IS UNIQUE",
            "CREATE CONSTRAINT brand_name IF NOT EXISTS FOR (b:Brand) REQUIRE b.name IS UNIQUE",
            "CREATE CONSTRAINT os_name IF NOT EXISTS FOR (o:OS) REQUIRE o.name IS UNIQUE",
            "CREATE CONSTRAINT chipset_name IF NOT EXISTS FOR (c:Chipset) REQUIRE c.name IS UNIQUE",
            "CREATE CONSTRAINT feature_name IF NOT EXISTS FOR (f:Feature) REQUIRE f.name IS UNIQUE",
        ]
        indexes = [
            "CREATE INDEX phone_year IF NOT EXISTS FOR (p:Phone) ON (p.yearReleased)",
            "CREATE INDEX phone_price IF NOT EXISTS FOR (p:Phone) ON (p.price)",
            "CREATE INDEX phone_brand IF NOT EXISTS FOR (p:Phone) ON (p.brand)",
            "CREATE INDEX battery_cap IF NOT EXISTS FOR (b:Battery) ON (b.capacityMah)",
            "CREATE INDEX camera_dxo IF NOT EXISTS FOR (c:Camera) ON (c.dxomarkScore)",
        ]
        for q in constraints + indexes:
            try: self.run(q)
            except: pass
        print("Schema ready.")

    def load_features(self):
        print("Loading Feature nodes...")
        features = list(FEATURE_RULES.keys()) + [
            "IP68","IP67","Stereo Speakers","3.5mm Jack",
            "Fingerprint Under Display","Fingerprint Side","Face ID",
            "Wi-Fi 6E","Wi-Fi 6","Bluetooth 5.3","OIS"
        ]
        for f in features:
            self.run("""
                MERGE (f:Feature {name: $name})
                SET f.category = $cat
            """, name=f, cat="Hardware" if f in ["3.5mm Jack","USB-C","Lightning"] else "Connectivity")
        print(f"  Created {len(features)} Feature nodes")

    def load_phone(self, row):
        r = row.to_dict()
        name     = clean(r.get("Phone Name"))
        brand    = clean(r.get("Brand"))
        yr       = clean(r.get("Year Released"))
        os_name  = clean(r.get("OS"))
        chipset  = clean(r.get("Chipset"))
        res      = clean(r.get("Display Resolution"))
        size     = clean(r.get("Display Size (inches)"))
        dtype    = clean(r.get("Display Type"))
        mcam     = clean(r.get("Main Camera"))
        fcam     = clean(r.get("Front Camera"))
        bat      = clean(r.get("Battery (mAh)"))
        charging = clean(r.get("Charging"))
        ram      = clean(r.get("RAM (GB)"))
        storage  = clean(r.get("Storage (GB)"))
        usb      = clean(r.get("USB Type"))
        weight   = clean(r.get("Weight (g)"))
        price    = clean(r.get("Price (USD)"))

        wired_ch   = extract_wired_charging(charging)
        wireless_ch = extract_wireless(charging)

        # 1. Phone node
        self.run("""
            MERGE (p:Phone {name: $name})
            SET p.yearReleased = $yr,
                p.price        = toInteger($price),
                p.weightG      = toInteger($weight),
                p.brand        = $brand,
                p.ram          = $ram,
                p.storage      = $storage,
                p.usbType      = $usb
        """, name=name, yr=yr, price=price, weight=weight,
             brand=brand, ram=ram, storage=storage, usb=usb)

        # 2. Brand node + MADE_BY
        self.run("""
            MERGE (b:Brand {name: $brand})
            WITH b
            MATCH (p:Phone {name: $phone})
            MERGE (p)-[:MADE_BY]->(b)
        """, brand=brand, phone=name)

        # 3. OS node + RUNS
        self.run("""
            MERGE (o:OS {name: $os})
            WITH o
            MATCH (p:Phone {name: $phone})
            MERGE (p)-[:RUNS]->(o)
        """, os=os_name, phone=name)

        # 4. Chipset node + HAS_CHIPSET
        self.run("""
            MERGE (c:Chipset {name: $chip})
            WITH c
            MATCH (p:Phone {name: $phone})
            MERGE (p)-[:HAS_CHIPSET]->(c)
        """, chip=chipset, phone=name)

        # 5. Display node + HAS_DISPLAY
        self.run("""
            MATCH (p:Phone {name: $phone})
            MERGE (d:Display {phoneId: $phone})
            SET d.sizeInches   = toFloat($size),
                d.type         = $dtype,
                d.resolution   = $res
            MERGE (p)-[:HAS_DISPLAY]->(d)
        """, phone=name, size=size, dtype=dtype, res=res)

        # 6. Battery node + HAS_BATTERY
        self.run("""
            MATCH (p:Phone {name: $phone})
            MERGE (b:Battery {phoneId: $phone})
            SET b.capacityMah     = toInteger($bat),
                b.wiredChargingW  = $wired,
                b.wirelessCharging = $wireless
            MERGE (p)-[:HAS_BATTERY]->(b)
        """, phone=name, bat=bat, wired=wired_ch, wireless=wireless_ch)

        # 7. Camera node + HAS_CAMERA
        self.run("""
            MATCH (p:Phone {name: $phone})
            MERGE (c:Camera {phoneId: $phone})
            SET c.mainCamera    = $mcam,
                c.frontCamera   = $fcam,
                c.dxomarkScore  = "N/A"
            MERGE (p)-[:HAS_CAMERA]->(c)
        """, phone=name, mcam=mcam, fcam=fcam)

        # 8. StorageVariant + HAS_VARIANT
        self.run("""
            MATCH (p:Phone {name: $phone})
            MERGE (sv:StorageVariant {phoneId: $phone, ramGb: $ram, storageGb: $sto})
            SET sv.price = toInteger($price)
            MERGE (p)-[:HAS_VARIANT]->(sv)
        """, phone=name, ram=ram, sto=storage, price=price)

        # 9. Features + SUPPORTS_FEATURE
        for feat_name, rule in FEATURE_RULES.items():
            try:
                if rule(r):
                    self.run("""
                        MATCH (p:Phone {name: $phone})
                        MATCH (f:Feature {name: $feat})
                        MERGE (p)-[:SUPPORTS_FEATURE]->(f)
                    """, phone=name, feat=feat_name)
            except: pass

    def load_all(self, csv_path):
        df = pd.read_csv(csv_path)
        print(f"\nLoading {len(df)} phones into Neo4j...")
        print("This will take a few minutes — 1 phone every ~0.1 seconds\n")

        self.load_features()

        success = 0
        errors  = 0
        for i, row in df.iterrows():
            try:
                self.load_phone(row)
                success += 1
                if (i+1) % 100 == 0:
                    print(f"  ✓ Loaded {i+1}/{len(df)} phones...")
            except Exception as e:
                errors += 1
                if errors <= 5:
                    print(f"  ✗ Error on {row.get('Phone Name','?')}: {e}")

        print(f"\n{'='*50}")
        print(f"✅ Done! {success} phones loaded, {errors} errors")
        print(f"{'='*50}")
        print(f"\nOpen Neo4j Browser: http://localhost:7474")
        print(f"Username: neo4j  |  Password: phonegraph2024")
        print(f"\nTest with: MATCH (p:Phone) RETURN count(p)")

    def print_stats(self):
        print("\n── Database Stats ──")
        labels = ["Phone","Brand","OS","Chipset","Display","Battery","Camera","Feature","StorageVariant"]
        for label in labels:
            result = self.run(f"MATCH (n:{label}) RETURN count(n) AS c")
            count = result.single()["c"]
            print(f"  {label}: {count} nodes")
        rels = self.run("MATCH ()-[r]->() RETURN count(r) AS c").single()["c"]
        print(f"  Relationships: {rels}")

# ── MAIN ──
if __name__ == "__main__":
    print("PhoneGraph Neo4j Loader")
    print("="*50)

    loader = PhoneGraphLoader(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)

    loader.clear_db()
    loader.create_schema()
    loader.load_all(CSV_FILE)
    loader.print_stats()
    loader.close()
