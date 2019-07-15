var headerMenu = widgetUtils.findObject(model.jsonModel, "id", "HEADER_APP_MENU_BAR");
if (headerMenu != null) {
    headerMenu.config.widgets.push({
        id: "HEADER_CUSTOM_AUDIT_LINK",
        name: "alfresco/menus/AlfMenuBarItem",
        config: {
            label: msg.get("header.domain.audit.title"),
            targetUrl: url.context + "/proxy/alfresco/com/domain/audit",
            targetUrlType: "FULL_PATH",
            targetUrlLocation: "NEW"
        }
    });
}