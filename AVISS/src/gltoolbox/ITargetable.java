package gltoolbox;

import processing.opengl.PJOGL;

public interface ITargetable {
	public Integer width = null;
	public Integer height = null;
	public void activate();
	public void clear(Integer mask);
	public ITargetable resize(int width, int height);
}
