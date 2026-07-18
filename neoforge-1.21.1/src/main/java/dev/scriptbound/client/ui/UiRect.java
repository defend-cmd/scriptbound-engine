package dev.scriptbound.client.ui;

public record UiRect(int x, int y, int w, int h)
{
    public boolean contains(double mx, double my)
    {
        return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
    }
}
