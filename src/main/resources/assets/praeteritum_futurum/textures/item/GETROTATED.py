from PIL import Image

base = Image.open("anomaly_base.png").convert("RGBA")
needle = Image.open("anomaly_needle.png").convert("RGBA")

#the pivot to the exact center of your chosen pixel block (e.g., pixel 7,7 -> center is 7.5, 7.5)
PIVOT_X = 7.5
PIVOT_Y = 7.5

for frame in range(32):
    angle = -(frame * 11.25)

    rotated = needle.rotate(
        angle,
        resample=Image.Resampling.NEAREST,
        center=(PIVOT_X, PIVOT_Y)
    )

    result = base.copy()
    result.alpha_composite(rotated)

    result.save(f"anomaly_compass_{frame:02d}.png")

print("Generated pixel-locked compass frames.")