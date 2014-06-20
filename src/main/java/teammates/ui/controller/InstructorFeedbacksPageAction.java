package teammates.ui.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import teammates.common.datatransfer.CourseDetailsBundle;
import teammates.common.datatransfer.EvaluationAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.util.Const;
import teammates.logic.api.GateKeeper;

public class InstructorFeedbacksPageAction extends Action {

    @Override
    protected ActionResult execute() throws EntityDoesNotExistException {
        //This can be null. Non-null value indicates the page is being loaded 
        //   to add a feedback to the specified course
        String courseIdForNewSession = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        
        new GateKeeper().verifyInstructorPrivileges(account);
                
        if (courseIdForNewSession!=null) {
            new GateKeeper().verifyAccessible(
                    logic.getInstructorForGoogleId(courseIdForNewSession, account.googleId), 
                    logic.getCourse(courseIdForNewSession), Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION);
        }

        InstructorFeedbacksPageData data = new InstructorFeedbacksPageData(account);
        data.courseIdForNewSession = courseIdForNewSession;
        // This indicates that an empty form to be shown (except possibly the course value filled in)
        data.newFeedbackSession = null; 

        data.instructors = new HashMap<String, InstructorAttributes>();
        data.courses = loadCoursesList(account.googleId, data.instructors);
        if (data.courses.size() == 0) {
            statusToUser.add(Const.StatusMessages.COURSE_EMPTY_IN_EVALUATION.replace("${user}", "?user="+account.googleId));
            data.existingEvalSessions = new ArrayList<EvaluationAttributes>();
            data.existingFeedbackSessions = new ArrayList<FeedbackSessionAttributes>();
        
        } else {
            data.existingEvalSessions = loadEvaluationsList(account.googleId);            
            data.existingFeedbackSessions = loadFeedbackSessionsList(account.googleId);
            if (data.existingFeedbackSessions.isEmpty() &&
                data.existingEvalSessions.isEmpty()) {
                statusToUser.add(Const.StatusMessages.EVALUATION_EMPTY);
            }
        }            
        
        EvaluationAttributes.sortEvaluationsByDeadlineDescending(data.existingEvalSessions);
        FeedbackSessionAttributes.sortFeedbackSessionsByCreationTimeDescending(data.existingFeedbackSessions);
        
        statusToAdmin = "Number of feedback sessions: "+data.existingFeedbackSessions.size();
        
        return createShowPageResult(Const.ViewURIs.INSTRUCTOR_FEEDBACKS, data);
    }
    
    protected List<FeedbackSessionAttributes> loadFeedbackSessionsList(
            String googleId) throws EntityDoesNotExistException {
        List<FeedbackSessionAttributes> sessions =
                logic.getFeedbackSessionsListForInstructor(googleId);
        
        return sessions;
    }

    protected List<EvaluationAttributes> loadEvaluationsList(String userId)
            throws EntityDoesNotExistException {
        List<EvaluationAttributes> evaluations =
                logic.getEvaluationsListForInstructor(userId);

        return evaluations;
    }
    
    protected List<CourseDetailsBundle> loadCoursesList(String userId, HashMap<String, InstructorAttributes> instructors)
            throws EntityDoesNotExistException {
        HashMap<String, CourseDetailsBundle> summary = logic.getCourseSummariesForInstructor(userId);
        List<CourseDetailsBundle>courses = new ArrayList<CourseDetailsBundle>(summary.values());
        List<CourseDetailsBundle> allowedCourses = new ArrayList<CourseDetailsBundle>();
        for (CourseDetailsBundle courseDetails : courses) {
            InstructorAttributes instructor = logic.getInstructorForGoogleId(courseDetails.course.id, account.googleId);
            instructors.put(courseDetails.course.id, instructor);
            if (instructor.isAllowedForPrivilege(Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION)) {
                allowedCourses.add(courseDetails);
            }
        }
        CourseDetailsBundle.sortDetailedCoursesByCourseId(allowedCourses);
        
        return allowedCourses;
    }

}
