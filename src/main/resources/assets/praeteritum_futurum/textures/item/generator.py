from PIL import Image

# Create a blank 16x512 canvas (16 width, 16*32 height)
sheet = Image.new("RGBA", (16, 16 * 32))

for i in range(32):
    frame = Image.open(f"anomaly_compass_{i:02d}.png")
    sheet.paste(frame, (0, i * 16))

# Save as the single texture file Minecraft expects
sheet.save("anomaly_compass.png")
print("Saved 16x512 anomaly_compass.png sprite sheet!")