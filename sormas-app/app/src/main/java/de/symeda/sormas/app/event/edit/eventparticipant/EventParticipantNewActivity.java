package de.symeda.sormas.app.event.edit.eventparticipant;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;

import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.app.BaseEditActivity;
import de.symeda.sormas.app.BaseEditFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.event.Event;
import de.symeda.sormas.app.backend.event.EventParticipant;
import de.symeda.sormas.app.backend.person.Person;
import de.symeda.sormas.app.component.menu.PageMenuItem;
import de.symeda.sormas.app.component.validation.FragmentValidator;
import de.symeda.sormas.app.core.async.AsyncTaskResult;
import de.symeda.sormas.app.core.async.SavingAsyncTask;
import de.symeda.sormas.app.core.async.TaskResultHolder;
import de.symeda.sormas.app.core.notification.NotificationHelper;
import de.symeda.sormas.app.person.SelectOrCreatePersonDialog;
import de.symeda.sormas.app.util.Bundler;
import de.symeda.sormas.app.util.Consumer;

import static de.symeda.sormas.app.core.notification.NotificationType.ERROR;

public class EventParticipantNewActivity extends BaseEditActivity<EventParticipant> {

    public static final String TAG = EventParticipantNewActivity.class.getSimpleName();

    private AsyncTask saveTask;

    private String eventUuid = null;

    public static void startActivity(Context context, String eventUuid) {
        BaseEditActivity.startActivity(context, EventParticipantNewActivity.class,buildBundle(eventUuid));
    }

    public static Bundler buildBundle(String eventUuid) {
        return buildBundle(null, 0).setEventUuid(eventUuid);
    }

    @Override
    protected void onCreateInner(Bundle savedInstanceState) {
        super.onCreateInner(savedInstanceState);
        eventUuid = new Bundler(savedInstanceState).getEventUuid();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        new Bundler(outState).setEventUuid(eventUuid);
    }

    @Override
    protected EventParticipant queryRootEntity(String recordUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected EventParticipant buildRootEntity() {
        Person person = DatabaseHelper.getPersonDao().build();
        EventParticipant eventParticipant = DatabaseHelper.getEventParticipantDao().build();
        eventParticipant.setPerson(person);
        return eventParticipant;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        getSaveMenu().setTitle(R.string.action_save_event);
        return result;
    }

    @Override
    protected BaseEditFragment buildEditFragment(PageMenuItem menuItem, EventParticipant activityRootData) {
        return EventParticipantNewFragment.newInstance(activityRootData);
    }

    @Override
    protected int getActivityTitle() {
        return R.string.heading_person_involved_new;
    }

    @Override
    public Enum getPageStatus() {
        return null;
    }

    @Override
    public void replaceFragment(BaseEditFragment f) {
        super.replaceFragment(f);
        getActiveFragment().setLiveValidationDisabled(true);
    }

    @Override
    public void saveData() {
        final EventParticipant eventParticipantToSave = (EventParticipant) getActiveFragment().getPrimaryData();
        EventParticipantNewFragment fragment = (EventParticipantNewFragment) getActiveFragment();

        if (fragment.isLiveValidationDisabled()) {
            fragment.disableLiveValidation(false);
        }

        try {
            FragmentValidator.validate(getContext(), fragment.getContentBinding());
        } catch (ValidationException e) {
            NotificationHelper.showNotification(this, ERROR, e.getMessage());
            return;
        }

        SelectOrCreatePersonDialog.selectOrCreatePerson(eventParticipantToSave.getPerson(), new Consumer<Person>() {
            @Override
            public void accept(Person person) {
                eventParticipantToSave.setPerson(person);

                saveTask = new SavingAsyncTask(getRootView(), eventParticipantToSave) {
                    @Override
                    protected void onPreExecute() {
                        showPreloader();
                    }

                    @Override
                    protected void doInBackground(TaskResultHolder resultHolder) throws Exception {
                        DatabaseHelper.getPersonDao().saveAndSnapshot(eventParticipantToSave.getPerson());
                        final Event event = DatabaseHelper.getEventDao().queryUuid(eventUuid);
                        eventParticipantToSave.setEvent(event);
                        DatabaseHelper.getEventParticipantDao().saveAndSnapshot(eventParticipantToSave);
                    }

                    @Override
                    protected void onPostExecute(AsyncTaskResult<TaskResultHolder> taskResult) {
                        hidePreloader();
                        super.onPostExecute(taskResult);
                        if (taskResult.getResultStatus().isSuccess()) {
                            EventParticipantEditActivity.startActivity(getContext(), getRootUuid(), eventUuid);
                        }
                    }
                }.executeOnThreadPool();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (saveTask != null && !saveTask.isCancelled())
            saveTask.cancel(true);
    }
}