package com.borqs.information.rest.bean;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("InformationList")
public class InformationList {
	private int total;
	private int count;
	private List<Information> informations;

	public InformationList() {
		informations = new ArrayList<Information>();
	}

	public InformationList(List<Information> informations) {
		this.informations = informations;
		this.count = informations.size();
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@XmlElement(name = "informations")
	public List<Information> getInformations() {
		return this.informations;
	}

	public void setInformations(List<Information> informations) {
		this.informations = informations;
		if(null!=informations) {
			count = informations.size();
		} else {
			count = 0;
		}
	}

	public String description() {
		StringBuilder sb = new StringBuilder();
		if(null != informations) {
			for(Information inf : informations) {
				sb.append(inf.toString()).append("\n");
			}
		}
		return sb.toString();
	}
}
