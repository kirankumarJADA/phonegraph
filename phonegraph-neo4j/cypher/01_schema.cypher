// ═══════════════════════════════════════════════════
// PHONEGRAPH — Neo4j Schema
// Run this FIRST before loading any data
// ═══════════════════════════════════════════════════

// ── CONSTRAINTS (ensure unique IDs) ──
CREATE CONSTRAINT phone_name IF NOT EXISTS
FOR (p:Phone) REQUIRE p.name IS UNIQUE;

CREATE CONSTRAINT brand_name IF NOT EXISTS
FOR (b:Brand) REQUIRE b.name IS UNIQUE;

CREATE CONSTRAINT os_name IF NOT EXISTS
FOR (o:OS) REQUIRE o.name IS UNIQUE;

CREATE CONSTRAINT chipset_name IF NOT EXISTS
FOR (c:Chipset) REQUIRE c.name IS UNIQUE;

CREATE CONSTRAINT feature_name IF NOT EXISTS
FOR (f:Feature) REQUIRE f.name IS UNIQUE;

// ── INDEXES (speed up queries) ──
CREATE INDEX phone_year IF NOT EXISTS
FOR (p:Phone) ON (p.yearReleased);

CREATE INDEX phone_price IF NOT EXISTS
FOR (p:Phone) ON (p.price);

CREATE INDEX phone_brand IF NOT EXISTS
FOR (p:Phone) ON (p.brand);

CREATE INDEX display_type IF NOT EXISTS
FOR (d:Display) ON (d.type);

CREATE INDEX battery_capacity IF NOT EXISTS
FOR (b:Battery) ON (b.capacityMah);

CREATE INDEX camera_dxo IF NOT EXISTS
FOR (c:Camera) ON (c.dxomarkScore);

CREATE INDEX chipset_manufacturer IF NOT EXISTS
FOR (c:Chipset) ON (c.manufacturer);

RETURN "Schema created successfully" AS status;
