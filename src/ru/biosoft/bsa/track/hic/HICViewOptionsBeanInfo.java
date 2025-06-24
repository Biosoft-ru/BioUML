package ru.biosoft.bsa.track.hic;

import java.util.stream.Stream;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class HICViewOptionsBeanInfo extends BeanInfoEx2<HICViewOptions> {
	public HICViewOptionsBeanInfo() {
		super(HICViewOptions.class);
	}

	@Override
	protected void initProperties() throws Exception {
		add("imageHeight");
		property("zoom")
			.tags(bean->Stream.of(bean.getAllZooms()))
			.add();
		
		property("normalization")
			.tags(bean->bean.getAllNormalizations().stream())
			.add();
		
		add("maxValue");
	}
}
