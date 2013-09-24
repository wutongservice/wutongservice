<?php
class InformationList
{
	public $total = 0;
	public $count = 0;
	public $informations;
	
	public function InformationList($informations)
	{
		if(null != $informations) {
			$this->informations = $informations;
			$this->count = count($informations);
		}
		else
		{
			$this->informations = array();	
		}
	}
}