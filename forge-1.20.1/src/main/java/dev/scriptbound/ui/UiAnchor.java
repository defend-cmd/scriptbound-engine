package dev.scriptbound.ui;

public enum UiAnchor
{
    TOP_LEFT(0F, 0F),
    TOP_CENTER(0.5F, 0F),
    TOP_RIGHT(1F, 0F),
    MIDDLE_LEFT(0F, 0.5F),
    CENTER(0.5F, 0.5F),
    MIDDLE_RIGHT(1F, 0.5F),
    BOTTOM_LEFT(0F, 1F),
    BOTTOM_CENTER(0.5F, 1F),
    BOTTOM_RIGHT(1F, 1F);

    public final float fx;
    public final float fy;

    UiAnchor(float fx, float fy)
    {
        this.fx = fx;
        this.fy = fy;
    }

    public static UiAnchor fromId(String id)
    {
        if (id == null)
        {
            return TOP_LEFT;
        }

        for (UiAnchor anchor : values())
        {
            if (anchor.name().equalsIgnoreCase(id))
            {
                return anchor;
            }
        }

        return TOP_LEFT;
    }

    public String id()
    {
        return this.name().toLowerCase();
    }

    public int resolveX(int parentX, int parentW, int w, int offsetX)
    {
        return Math.round(parentX + this.fx * parentW - this.fx * w) + offsetX;
    }

    public int resolveY(int parentY, int parentH, int h, int offsetY)
    {
        return Math.round(parentY + this.fy * parentH - this.fy * h) + offsetY;
    }
}
