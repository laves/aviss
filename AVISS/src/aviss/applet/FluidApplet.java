package aviss.applet;
import processing.core.*;
import diewald_fluid.Fluid2D;
import diewald_fluid.Fluid2D_CPU;
import diewald_fluid.Fluid2D_GPU;
import processing.opengl.*;
import codeanticode.glgraphics.*;

public class FluidApplet extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int fluid_size_x = 192;
	int fluid_size_y = 108;

	int cell_size = 10;
	int window_size_x = fluid_size_x * cell_size + (cell_size * 2);
	int window_size_y = fluid_size_y * cell_size + (cell_size * 2);

	Fluid2D fluid;
	PImage baseTexture;
	PImage output_densityMap;
	
	public static void main(String args[]) {
		PApplet.main(new String[] { "--present", FluidApplet.class.getName() });
	}

	public void setup() {
		
		size(window_size_x, window_size_y, GLConstants.GLGRAPHICS);
		fluid = createFluidSolver();
		// frameRate(60);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void draw() {
		background(255);
		
		// Influence		
		// 1) set barriers
		
		
		// 2) set emitters
		
		
		// 3) set velocites
		
		
//		if (mousePressed)
//			fluidInfluence(fluid);		
				
		// Emitters!
		setVel(fluid, 10, 10, 2, 2, .2f, .2f);
		setDens(fluid, 10, 10, 8, 8, 4, 0, 0);

		setVel(fluid, width - 10, cell_size * 4, 2, 2, -.2f, .2f);
		setDens(fluid, width - 10, cell_size * 4, 8, 8, 4, 4, 4);

		setVel(fluid, width - 10, height - 10, 2, 2, -.2f, -.2f);
		setDens(fluid, width - 10, height - 10, 8, 8, 0, 0, 4);

		setVel(fluid, 10, height - 10, 2, 2, .2f, -.2f);
		setDens(fluid, 10, height - 10, 8, 8, 0, 4, 0);

		// update
		fluid.update();
		
		//render
		image(fluid.getDensityMap(), 0, 0, width, height);

		//println(frameRate);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// createFluidSolver();
	//
	Fluid2D createFluidSolver() {
		Fluid2D fluid_tmp = new Fluid2D_GPU(this, fluid_size_x, fluid_size_y);																		
		fluid_tmp.setParam_Timestep(0.15f);
		fluid_tmp.setParam_Iterations(16);
		fluid_tmp.setParam_IterationsDiffuse(1);
		fluid_tmp.setParam_Viscosity(0.00000001f);
		fluid_tmp.setParam_Diffusion(0.0000000001f);
		fluid_tmp.setParam_Vorticity(3.0f);
		fluid_tmp.processDensityMap(true);
		fluid_tmp.processDiffusion(true);
		fluid_tmp.processViscosity(true);
		fluid_tmp.processVorticity(true);
		fluid_tmp.processDensityMap(true);
		fluid_tmp.smoothDensityMap(true);
		fluid_tmp.setObjectsColor(1, 1, 1, 1);

		output_densityMap = createImage(window_size_x, window_size_y, RGB);
		fluid_tmp.setDensityMap(output_densityMap);
		return fluid_tmp;
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// fluidInfluence();
	//
//	public void fluidInfluence(Fluid2D fluid2d) {
//		if (mouseButton == LEFT) {
//			if (edit_quader) {
//				int quader_size = 2;
//				int xpos = (int) (mouseX / (float) cell_size) - quader_size / 2;
//				int ypos = (int) (mouseY / (float) cell_size) - quader_size / 2;
//				addObject(fluid2d, xpos, ypos, quader_size, quader_size, 0);
//			} else {
//				setDens(fluid2d, mouseX, mouseY, 8, 8, 2, 2, 2);
//			}
//		}
//		if (mouseButton == CENTER) {
//			if (edit_quader) {
//				int quader_size = 2;
//				int xpos = (int) (mouseX / (float) cell_size) - quader_size / 2;
//				int ypos = (int) (mouseY / (float) cell_size) - quader_size / 2;
//				addObject(fluid2d, xpos, ypos, quader_size, quader_size, 1);
//			} else {
//				setDens(fluid2d, mouseX, mouseY, 8, 8, 4, 0, 0);
//			}
//		}
//		if (mouseButton == RIGHT) {
//			float vel_fac = 0.13f;
//			int size = (int) (((fluid_size_x + fluid_size_y) / 2.0) / 50.0);
//			size = max(size, 1);
//			setVel(fluid2d, mouseX, mouseY, size, size, (mouseX - pmouseX)
//					* vel_fac, (mouseY - pmouseY) * vel_fac);
//		}
//	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// setDens();
	//
	void setDens(Fluid2D fluid2d, int x, int y, int sizex, int sizey, float r,
			float g, float b) {
		for (int y1 = 0; y1 < sizey; y1++) {
			for (int x1 = 0; x1 < sizex; x1++) {
				int xpos = (int) (x / (float) cell_size) + x1 - sizex / 2;
				int ypos = (int) (y / (float) cell_size) + y1 - sizey / 2;
				fluid2d.addDensity(0, xpos, ypos, r);
				fluid2d.addDensity(1, xpos, ypos, g);
				fluid2d.addDensity(2, xpos, ypos, b);
			}
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// setVel();
	//
	void setVel(Fluid2D fluid2d, int x, int y, int sizex, int sizey,
			float velx, float vely) {
		for (int y1 = 0; y1 < sizey; y1++) {
			for (int x1 = 0; x1 < sizex; x1++) {
				int xpos = (int) ((x / (float) cell_size)) + x1 - sizex / 2;
				int ypos = (int) ((y / (float) cell_size)) + y1 - sizey / 2;
				fluid2d.addVelocity(xpos, ypos, velx, vely);
			}
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// addObject();
	//
	public void addObject(Fluid2D fluid2d, int posx, int posy, int sizex,
			int sizey, int mode) {
		int offset = 0;
		int xlow = posx;
		int xhig = posx + sizex;
		int ylow = posy;
		int yhig = posy + sizey;

		for (int x = xlow - offset; x < xhig + offset; x++) {
			for (int y = ylow - offset; y < yhig + offset; y++) {
				if (x < 0 || x >= fluid2d.getSizeXTotal() || y < 0
						|| y >= fluid2d.getSizeYTotal())
					continue;
				if (mode == 0)
					fluid2d.addObject(x, y);
				if (mode == 1)
					fluid2d.removeObject(x, y);
			}
		}
	}

}
