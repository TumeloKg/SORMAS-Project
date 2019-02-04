package de.symeda.sormas.ui.therapy;

import java.util.List;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionModel.HasUserSelectionAllowed;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.therapy.TreatmentCriteria;
import de.symeda.sormas.api.therapy.TreatmentIndexDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.utils.AbstractGrid;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class TreatmentGrid extends Grid implements AbstractGrid<TreatmentCriteria> {

	private static final String EDIT_BTN_ID = "edit";
	
	private TreatmentCriteria treatmentCriteria = new TreatmentCriteria();
	
	public TreatmentGrid() {
		setSizeFull();

		if (UserProvider.getCurrent().hasUserRight(UserRight.PERFORM_BULK_OPERATIONS)) {
			setSelectionMode(SelectionMode.MULTI);
		} else {
			setSelectionMode(SelectionMode.NONE);
		}

		BeanItemContainer<TreatmentIndexDto> container = new BeanItemContainer<>(TreatmentIndexDto.class);
		GeneratedPropertyContainer generatedContainer = new GeneratedPropertyContainer(container);
        VaadinUiUtil.addIconColumn(generatedContainer, EDIT_BTN_ID, FontAwesome.PENCIL_SQUARE);
		setContainerDataSource(generatedContainer);
		
		setColumns(EDIT_BTN_ID, TreatmentIndexDto.TREATMENT_TYPE, TreatmentIndexDto.TREATMENT_DATE_TIME,
				TreatmentIndexDto.DOSE, TreatmentIndexDto.ROUTE, TreatmentIndexDto.EXECUTING_CLINICIAN);
		
		getColumn(EDIT_BTN_ID).setRenderer(new HtmlRenderer());
		getColumn(EDIT_BTN_ID).setWidth(60);
		getColumn(TreatmentIndexDto.TREATMENT_DATE_TIME).setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat()));
		
		for (Column column : getColumns()) {
			column.setHeaderCaption(I18nProperties.getPrefixCaption(
					TreatmentIndexDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
		}

		addItemClickListener(e -> {
			if (e.getPropertyId() == null) {
				return;
			}
			
			if (EDIT_BTN_ID.equals(e.getPropertyId()) || e.isDoubleClick()) {
				ControllerProvider.getTherapyController().openTreatmentEditForm((TreatmentIndexDto) e.getItemId(), this::reload);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public BeanItemContainer<TreatmentIndexDto> getContainer() {
		GeneratedPropertyContainer container = (GeneratedPropertyContainer) super.getContainerDataSource();
		return (BeanItemContainer<TreatmentIndexDto>) container.getWrappedContainer();
	}
	
	public void reload() {
		if (getSelectionModel() instanceof HasUserSelectionAllowed) {
			deselectAll();
		}

		List<TreatmentIndexDto> entries = FacadeProvider.getTherapyFacade().getTreatmentIndexList(treatmentCriteria);
		
		getContainer().removeAllItems();
		getContainer().addAll(entries);
	}
	
	@Override
	public void setCriteria(TreatmentCriteria treatmentCriteria) {
		this.treatmentCriteria = treatmentCriteria;
	}
	
	@Override
	public TreatmentCriteria getCriteria() {
		return treatmentCriteria;
	}	
	
}