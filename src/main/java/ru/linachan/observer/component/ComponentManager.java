package ru.linachan.observer.component;

import ru.linachan.common.GenericManager;

public class ComponentManager extends GenericManager<Component> {

    @Override
    protected void onDiscover(Class<? extends Component> object) {
        try {
            Component component = object.newInstance();
            objects.put(object, component);
            component.onInit();
            logger.info("Component[{}]: Initialized", object.getSimpleName());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Component[{}]: Unable to initialize: {}", object.getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void onShutDown() {
        objects.keySet().stream()
            .filter(component -> objects.get(component) != null)
            .forEach(component -> objects.get(component).onShutDown());
    }
}
