// ═══════════════════════════════════════════════════
// PHONEGRAPH — Sample Cypher Queries
// Use these to test your KG after loading data
// ═══════════════════════════════════════════════════

// 1. Find all phones by Samsung with 5G
MATCH (p:Phone)-[:MADE_BY]->(b:Brand {name: "Samsung"})
MATCH (p)-[:SUPPORTS_FEATURE]->(f:Feature {name: "5G"})
RETURN p.name, p.yearReleased, p.price
ORDER BY p.price DESC
LIMIT 10;

// 2. Best camera phones (by DXOMark score)
MATCH (p:Phone)-[:HAS_CAMERA]->(c:Camera)
WHERE c.dxomarkScore <> "N/A"
RETURN p.name, p.brand, c.dxomarkScore, c.mainCamera
ORDER BY toInteger(c.dxomarkScore) DESC
LIMIT 10;

// 3. Phones under £400 with 5G and NFC
MATCH (p:Phone)-[:SUPPORTS_FEATURE]->(f1:Feature {name: "5G"})
MATCH (p)-[:SUPPORTS_FEATURE]->(f2:Feature {name: "NFC"})
WHERE toInteger(p.price) < 400
RETURN p.name, p.brand, p.price, p.yearReleased
ORDER BY toInteger(p.price) ASC;

// 4. Compare two phones
MATCH (p1:Phone {name: "Samsung Galaxy S24 Ultra"})
MATCH (p2:Phone {name: "Apple iPhone 15 Pro Max"})
MATCH (p1)-[:HAS_CAMERA]->(c1:Camera)
MATCH (p2)-[:HAS_CAMERA]->(c2:Camera)
MATCH (p1)-[:HAS_BATTERY]->(b1:Battery)
MATCH (p2)-[:HAS_BATTERY]->(b2:Battery)
RETURN p1.name, c1.dxomarkScore AS s24_dxo, b1.capacityMah AS s24_battery,
       p2.name, c2.dxomarkScore AS ip15_dxo, b2.capacityMah AS ip15_battery;

// 5. Phones on Snapdragon 8 Gen 3
MATCH (p:Phone)-[:HAS_CHIPSET]->(c:Chipset {name: "Snapdragon 8 Gen 3"})
RETURN p.name, p.brand, p.price
ORDER BY p.brand;

// 6. Count phones per brand
MATCH (p:Phone)-[:MADE_BY]->(b:Brand)
RETURN b.name AS brand, count(p) AS total_phones
ORDER BY total_phones DESC;

// 7. Phones with biggest battery
MATCH (p:Phone)-[:HAS_BATTERY]->(b:Battery)
RETURN p.name, p.brand, b.capacityMah
ORDER BY toInteger(b.capacityMah) DESC
LIMIT 10;

// 8. All storage variants for a phone
MATCH (p:Phone {name: "Samsung Galaxy S24 Ultra"})-[:HAS_VARIANT]->(sv:StorageVariant)
RETURN sv.ramGb, sv.storageGb, sv.price
ORDER BY sv.price;
