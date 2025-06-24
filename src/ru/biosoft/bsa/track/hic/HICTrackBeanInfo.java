package ru.biosoft.bsa.track.hic;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.bsa.ChrNameMapping.ChrMappingSelector;

public class HICTrackBeanInfo extends BeanInfoEx {
	public HICTrackBeanInfo() {
		super(HICTrack.class, true);
	}

	@Override
	public void initProperties() throws Exception {
		add("genomeSelector");
		property("chrMapping").canBeNull().simple().editor(ChrMappingSelector.class).add();
	}
}
