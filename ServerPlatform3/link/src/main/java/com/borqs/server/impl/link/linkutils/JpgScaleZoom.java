package com.borqs.server.impl.link.linkutils;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

public class JpgScaleZoom extends JpgZoom {

	private String sFile = "";
	private String dFile = "";
	private String scaleType = "width";
	private int    scaleNum  = 100;
	private int nw = -1;
	private int nh = -1;
	protected boolean originalShape = false;
	
	public JpgScaleZoom() {}
	
	public JpgScaleZoom(String _sFile, String _dFile) {
	
		this.sFile = _sFile;
		this.dFile = _dFile;
	}
	
	public JpgScaleZoom(String _sFile, String _dFile, String _scaleType) {
		
		this(_sFile, _dFile);
		
		if (_scaleType.equals("width") || _scaleType.equals("height") || _scaleType.equals("auto")) {
			
			this.scaleType = _scaleType;
		}
	}
	
	public JpgScaleZoom(String _sFile, String _dFile, String _scaleType, int _scaleNum) {
	
		this(_sFile, _dFile, _scaleType);
		
		if (_scaleNum >= 0) {
			
			this.scaleNum = _scaleNum;
		}
	}
		
	
	public void makeImg() {
		
		try {
			
			File fi = new File(this.sFile);
			File fo = new File(this.dFile);
			
			AffineTransform transform = new AffineTransform(); 
			BufferedImage bis         = ImageIO.read(fi); 
			
			int w = bis.getWidth();
			int h = bis.getHeight();
			double scale = (double)w / h;
			
			if(this.scaleType.equals("auto"))
			{
				this.scaleType = (w < h) ? "width" : "height";
			}
			
				if (this.scaleType.equals("width")) {
					
					System.out.println("zoom by width:" + this.scaleNum);
					this.nw = this.scaleNum;
					this.nh = (this.nw * h) / w ;
				} else if (this.scaleType.equals("height")) {
				
					System.out.println("zoom by height:" + this.scaleNum);
					this.nh = this.scaleNum;
					this.nw = (this.nh * w) / h ;
				} 
				
				if (this.originalShape && this.nw > w && this.nh > h) {

					System.out.println("zoom original shape");
					this.nw = w;
					this.nh = h;
				}
			//super.jpgZoom(this.sFile, this.dFile, this.nw, this.nh);
			super.sFile = this.sFile;
			super.dFile = this.dFile;
			super.nw    = this.nw;
			super.nh    = this.nh;
			super.makeImg();
		} catch(Exception e) {
			
			e.printStackTrace(); 
		}
	}
	
	/*public static void main(String[] args) {
	
		String _sFile     = String.valueOf(args[0]);
		String _dFile     = String.valueOf(args[1]);
		String _scaleType = String.valueOf(args[2]);
		int _scaleNum     = Integer.parseInt(args[3]);
		
		jpgScaleZoom test = new jpgScaleZoom(_sFile, _dFile, _scaleType, _scaleNum);
		test.makeImg();
	}*/
}
