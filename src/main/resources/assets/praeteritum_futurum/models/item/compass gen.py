for i in range(32):
    json_content = f"""{{
  "parent": "minecraft:item/generated",
  "textures": {{
    "layer0": "praeteritum_futurum:item/anomaly_compass_{i:02d}"
  }}
}}"""
    with open(f"anomaly_compass_{i:02d}.json", "w") as f:
        f.write(json_content)

print("Generated 32 compass model JSON files.")