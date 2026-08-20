package com.bepa.eis.server.api.DTO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a menu structure with main menu items and their sub-items.
 */
public class Menu {

    private final List<MainMenuItem> mainMenuItems = new ArrayList<>();

    public MainMenuItem addMainMenuItem(
            int menuItemId,
            String menuItemText,
            String menuItemUrl,
            int displayOrder,
            Integer iconId,
            String menuItemType,
            String iconSvg,
            String description
    ) {
        MainMenuItem mainMenuItem = new MainMenuItem(menuItemId, menuItemText, menuItemUrl, displayOrder, iconId, menuItemType, iconSvg, description);
        mainMenuItems.add(mainMenuItem);
        return mainMenuItem;
    }

    /**
     * Converts the menu structure to an XML document.
     * @return
     * @throws ParserConfigurationException
     */
    public Document toXmlDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("menu");
        doc.appendChild(root);

        for (MainMenuItem m : mainMenuItems) {
            Element mainMenuEl = doc.createElement("main-menu-item");
            root.appendChild(mainMenuEl);

            Element displayEL = doc.createElement("display");
            displayEL.setTextContent(String.valueOf(m.menuItemText));
            mainMenuEl.appendChild(displayEL);

            if (m.menuItemType != null) {
                Element typeEL = doc.createElement("menuItemType");
                typeEL.setTextContent(String.valueOf(m.menuItemType));
                mainMenuEl.appendChild(typeEL);
            }

            if (m.iconId != null) {
                Element iconIdEL = doc.createElement("iconId");
                iconIdEL.setTextContent(String.valueOf(m.iconId));
                mainMenuEl.appendChild(iconIdEL);
            }

            if (m.iconSvg != null && !m.iconSvg.isEmpty()) {
                Element iconSvgEL = doc.createElement("iconSvg");
                iconSvgEL.setTextContent(m.iconSvg);
                mainMenuEl.appendChild(iconSvgEL);
            }

            if (m.menuItemUrl != null && ! m.menuItemUrl.isEmpty()) {
                Element urlEL = doc.createElement("url");
                urlEL.setTextContent(String.valueOf(m.menuItemUrl));
                mainMenuEl.appendChild(urlEL);
            }

            if (m.description != null && !m.description.isEmpty()) {
                Element descEL = doc.createElement("description");
                descEL.setTextContent(m.description);
                mainMenuEl.appendChild(descEL);
            }

            Element displayOrderEL = doc.createElement("displayOrder");
            displayOrderEL.setTextContent(String.valueOf(m.displayOrder));
            mainMenuEl.appendChild(displayOrderEL);

            for (SubMenuItem s : m.subMenuItems) {
                Element subMenuEl = doc.createElement("submain-menu-item");
                mainMenuEl.appendChild(subMenuEl);

                Element subMenuDdisplayEL = doc.createElement("display");
                subMenuDdisplayEL.setTextContent(String.valueOf(s.menuItemText));
                subMenuEl.appendChild(subMenuDdisplayEL);



                if (s.menuItemType != null) {
                    Element subMenuTypeEL = doc.createElement("menuItemType");
                    subMenuTypeEL.setTextContent(String.valueOf(s.menuItemType));
                    subMenuEl.appendChild(subMenuTypeEL);
                }

                if (s.iconId != null) {
                    Element subMenuIconIdEL = doc.createElement("iconId");
                    subMenuIconIdEL.setTextContent(String.valueOf(s.iconId));
                    subMenuEl.appendChild(subMenuIconIdEL);
                }

                if (s.iconSvg != null && !s.iconSvg.isEmpty()) {
                    Element subMenuIconSvgEL = doc.createElement("iconSvg");
                    subMenuIconSvgEL.setTextContent(s.iconSvg);
                    subMenuEl.appendChild(subMenuIconSvgEL);
                }

                Element subMenuUrlEL = doc.createElement("url");
                subMenuUrlEL.setTextContent(String.valueOf(s.menuItemUrl));
                subMenuEl.appendChild(subMenuUrlEL);

                if (s.description != null && !s.description.isEmpty()) {
                    Element subMenuDescEL = doc.createElement("description");
                    subMenuDescEL.setTextContent(s.description);
                    subMenuEl.appendChild(subMenuDescEL);
                }

                Element subMenuDisplayOrderEL = doc.createElement("displayOrder");
                subMenuDisplayOrderEL.setTextContent(String.valueOf(s.displayOrder));
                subMenuEl.appendChild(subMenuDisplayOrderEL);
            }

        }

        return doc;
    }

    public class MainMenuItem {
        private final int menuItemId;
        private final String menuItemText;
        private final String menuItemUrl;
        private final int displayOrder;
        private final Integer iconId;
        private final String iconSvg;
        private final String menuItemType;
        private final String description;
        private final List<SubMenuItem> subMenuItems = new ArrayList<>();

        private MainMenuItem(
                int menuItemId,
                String menuItemText,
                String menuItemUrl,
                int displayOrder,
                Integer iconId,
                String menuItemType,
                String iconSvg,
                String description
        ) {
            this.menuItemId = menuItemId;
            this.menuItemText = menuItemText;
            this.menuItemUrl = menuItemUrl;
            this.displayOrder = displayOrder;
            this.iconId = iconId;
            this.iconSvg = iconSvg;
            this.menuItemType = menuItemType;
            this.description = description;
        }

        public int getMenuItemId() {
            return menuItemId;
        }

        public void addSubMenuItem(
                int menuItemId,
                String menuItemText,
                String menuItemUrl,
                int parentMenuItemId,
                int displayOrder,
                Integer iconId,
                String menuItemType,
                String iconSvg,
                String description
        ) {
            SubMenuItem subMenuItem = new SubMenuItem(menuItemId, menuItemText, menuItemUrl, parentMenuItemId, displayOrder, iconId, menuItemType, iconSvg, description);
            subMenuItems.add(subMenuItem);
        }
    }

    private class SubMenuItem {
        private final int menuItemId;
        private final String menuItemText;
        private final String menuItemUrl;
        private final int parentMenuItemId;
        private final int displayOrder;
        private final Integer iconId;
        private final String iconSvg;
        private final String menuItemType;
        private final String description;

        private SubMenuItem(
                int menuItemId,
                String menuItemText,
                String menuItemUrl,
                int parentMenuItemId,
                int displayOrder,
                Integer iconId,
                String menuItemType,
                String iconSvg,
                String description
        ) {
            this.menuItemId = menuItemId;
            this.menuItemText = menuItemText;
            this.menuItemUrl = menuItemUrl;
            this.parentMenuItemId = parentMenuItemId;
            this.displayOrder = displayOrder;
            this.iconId = iconId;
            this.iconSvg = iconSvg;
            this.menuItemType = menuItemType;
            this.description = description;
        }
    }

}
