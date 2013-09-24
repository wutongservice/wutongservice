package com.borqs.server.base.util.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

public class JpgZoom {

	protected String sFile = "";
	protected String dFile = "";
	protected int nw = -1;
	protected int nh = -1;
	protected int defaultNw = 80;
	protected int defaultNh = 80;
	
	public JpgZoom() {};
	
	public JpgZoom(String _sFile, String _dFile) {
	
		this.sFile = _sFile;
		this.dFile = _dFile;
	}

	public JpgZoom(String _sFile, String _dFile, int _nw, int _nh) {
	
		this(_sFile, _dFile);
		this.setNw(_nw);
		this.setNh(_nh);
	}
	
	public JpgZoom(String _sFile, String _dFile, int _nw, int _nh, int _defaultNw, int _defaultNh) {
	
		this(_sFile, _dFile, _nw, _nh);
		this.setDefaultNw(_defaultNw);
		this.setDefaultNh(_defaultNh);
	}
	
	protected void setNw (int _nw) {
	
		if (_nw == -1) {
		
			this.nw = this.defaultNw;
		} else {
		
			this.nw = _nw;
		}
	}
	
	protected void setNh (int _nh) {
	
		if (_nh == -1) {
		
			this.nh = this.defaultNh;
		} else {
		
			this.nh = _nh;
		}
	}
	
	protected void setDefaultNw (int _nw) {
	
		this.defaultNw = _nw;
	}
	
	protected void setDefaultNh (int _nh) {
	
		this.defaultNh = _nh;
	}
	
	public void makeImg() {
	
		try {
			
			File fi = new File(this.sFile); //��ͼ�ļ� 
			File fo = new File(this.dFile); //��Ҫת������Сͼ�ļ� 
			
			AffineTransform transform = new AffineTransform(); 
			BufferedImage bis         = ImageIO.read(fi); 
			
			int w = bis.getWidth(); 		//ԭͼƬ�Ŀ�
			int h = bis.getHeight(); 		//ԭͼƬ�ĸ�
			double sx = (double)this.nw / w;
			double sy = (double)this.nh / h; 
			transform.setToScale(sx, sy); 
			
			System.out.println("Old Width and Height is: " + w + " " + h); 
			System.out.println("New Width and Height is: " + this.nw + " " + this.nh); 
			System.out.println(""); 
			
//			AffineTransformOp ato = new AffineTransformOp(transform, null); 
//			BufferedImage bid     = new BufferedImage(this.nw, nh, BufferedImage.TYPE_3BYTE_BGR); 
//			ato.filter(bis, bid); 
//			ImageIO.write(bid, "jpg", fo); 
			
						 
			//create new image
			BufferedImage bid     = new BufferedImage(this.nw, nh, BufferedImage.TYPE_3BYTE_BGR);
			 
			//draw the source image transformed onto the destination
			Graphics2D g2 = bid.createGraphics();
			g2.drawImage(bis,transform,null);
			g2.dispose();
			ImageIO.write(bid, "jpg", fo);
		} catch(Exception e) {
			
			e.printStackTrace(); 
		}
	}
	
	/*public static void main(String[] args) {
	
		String _sFile = String.valueOf(args[0]);
		String _dFile = String.valueOf(args[1]);
		int    _nw    = Integer.parseInt(args[2]);
		int    _nh    = Integer.parseInt(args[3]);
		
		JpgZoom test = new JpgZoom(_sFile, _dFile, _nw, _nh);
		test.makeImg();
	}*/
}
