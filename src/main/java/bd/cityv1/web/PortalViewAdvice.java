package bd.cityv1.web;

import bd.cityv1.storage.ImageDelivery;
import bd.cityv1.validation.InputRules;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import java.beans.PropertyEditorSupport;

@ControllerAdvice
@RequiredArgsConstructor
public class PortalViewAdvice {

    private final ImageDelivery imageDelivery;

    @ModelAttribute("imageDelivery")
    public ImageDelivery imageDelivery() {
        // URL attributes run in Thymeleaf's restricted context, which rejects bean lookups.
        return imageDelivery;
    }

    @InitBinder
    public void normalizeFormFields(WebDataBinder binder) {
        for (String field : new String[]{"name", "nationalId", "occupation", "street", "city", "zipCode",
                "address.street", "address.city", "address.zipCode", "department", "title", "description",
                "category", "location", "costNotes"}) {
            binder.registerCustomEditor(String.class, field, new StringTrimmerEditor(false));
        }
        binder.registerCustomEditor(String.class, "phone", new PropertyEditorSupport() {
            @Override public void setAsText(String value) { setValue(InputRules.phone(value)); }
        });
        binder.registerCustomEditor(String.class, "email", new PropertyEditorSupport() {
            @Override public void setAsText(String value) { setValue(InputRules.email(value)); }
        });
    }
}
