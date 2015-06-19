package org.exoplatform.chat.portlet.chat;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.GroupCalendarData;
import org.exoplatform.chat.utils.ChatUtils;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Named("calendarService")
@ApplicationScoped
public class CalendarService {

  org.exoplatform.calendar.service.CalendarService calendarService_;
  OrganizationService organizationService_;
  private static final Logger LOG = Logger.getLogger("CalendarService");

  @Inject
  public CalendarService(org.exoplatform.calendar.service.CalendarService calendarService, OrganizationService organizationService)
  {
    calendarService_ = calendarService;
    organizationService_ = organizationService;
  }

  protected void saveEvent(String user, String calName, String users, String summary,
                         Date from, Date to, String location) throws Exception
  {
    if (!"".equals(users)) {
      String[] participants = users.split(",");
      for (String participant:participants) {
        String calId = getFirstCalendarsId(participant);
        saveEvent(participant, false, participants, calId, calName, summary, from, to, location);
      }
    } else {
      String calId = getCalendarId(user, calName);
      saveEvent(user, true, null, calId, calName, summary, from, to, location);
    }
  }

  protected void saveEvent(String user, boolean isPublic, String[] participants, String calId, String calName, String summary,
                         Date from, Date to, String location) throws Exception
  {
    summary = ChatUtils.escapeSpecialCharacters(summary);
    location = ChatUtils.escapeSpecialCharacters(location);
    if (calId!=null) {
      CalendarEvent event = new CalendarEvent();
      event.setCalendarId(calId);
      event.setSummary(summary);
      event.setEventType(CalendarEvent.TYPE_EVENT);
      event.setRepeatType(CalendarEvent.RP_NOREPEAT);
      event.setPrivate(true);
      event.setFromDateTime(from);
      event.setToDateTime(to);
      event.setPriority(CalendarEvent.PRIORITY_NORMAL);
      event.setLocation(location);
      if (isPublic)
        calendarService_.savePublicEvent(calId, event, true);
      else {
        if (participants!=null) {
          event.setParticipant(participants);
          String[] participantsStatus = new String[participants.length];
          for(int i=0 ; i<participants.length ; i++) {
            participantsStatus[i] = participants[i]+":confirmed";
          }
        event.setParticipantStatus(participantsStatus);

        }
        calendarService_.saveUserEvent(user, calId, event, true);
//        calendarService_.confirmInvitation("john", "benjamin", "john", Utils.PRIVATE_TYPE, calId, eventId, Utils.ACCEPT);
      }
    }
  }

  protected void saveTask(String currentUser, String username, String summary, String roomName, String isSpace,
                          Date from, Date to) throws Exception {
    summary = ChatUtils.escapeSpecialCharacters(summary);
    CalendarEvent task = new CalendarEvent();
    task.setSummary(summary);
    task.setEventType(CalendarEvent.TYPE_TASK);
    task.setRepeatType(CalendarEvent.RP_NOREPEAT);
    task.setEventState(CalendarEvent.NEEDS_ACTION);
    task.setPrivate(true);
    task.setFromDateTime(from);
    task.setToDateTime(to);
    task.setPriority(CalendarEvent.PRIORITY_NORMAL);
    task.setTaskDelegator(username);
    task.setDescription("Created by " + currentUser + " for " + username);
    String calId = null;
    if ("true".equals(isSpace)) {
      calId = getCalendarId(currentUser, roomName);
    }
    if (calId != null) {
      task.setCalendarId(calId);
      calendarService_.savePublicEvent(calId, task, true);
    } else {
      if (!StringUtils.isEmpty(username)) {
        String[] assignees = username.split(",");
        for (String assignee : assignees) {
          String assigneeCalId = getFirstCalendarsId(assignee);
          task.setCalendarId(assigneeCalId);
          calendarService_.saveUserEvent(assignee, assigneeCalId, task, true);
        }
      }
    }
  }

  private String getFirstCalendarsId(String username) {


    StringBuilder sb = new StringBuilder();
    List<org.exoplatform.calendar.service.Calendar> listUserCalendar = null;
    try {
      listUserCalendar = calendarService_.getUserCalendars(username, true);
      if (listUserCalendar.size()>0) {
        return listUserCalendar.get(0).getId();
      }
    } catch (Exception e) {
      LOG.info("Error while checking User Calendar :" + e.getMessage());
    }
    return null;
  }

  private String getCalendarId(String username, String space) {

    String id = null;
    StringBuilder sb = new StringBuilder();
    List<GroupCalendarData> listgroupCalendar = null;
    try {
      listgroupCalendar = calendarService_.getGroupCalendars(getUserGroups(username), true, username);
    } catch (Exception e) {
      LOG.info("Error while checking User Calendar :" + e.getMessage());
    }
    for (GroupCalendarData g : listgroupCalendar) {
      for (org.exoplatform.calendar.service.Calendar c : g.getCalendars()) {
        if (space.equals(c.getName())) {
          id = c.getId();
        }
      }
    }
    return id;
  }

  private String[] getUserGroups(String username) throws Exception {

    Object[] objs = organizationService_.getGroupHandler().findGroupsOfUser(username).toArray();
    String[] groups = new String[objs.length];
    for (int i = 0; i < objs.length; i++) {
      groups[i] = ((Group) objs[i]).getId();
    }
    return groups;
  }
}
