// ----------------------------------------- //
// Coded by Adam Berlak, ID: 30008230 //
// ----------------------------------------- //

// ----- Import Utilities ----- //

import java.util.Arrays;
import java.util.LinkedList;

// ----- Object used for Evaluating Given Soft Constraints for a Timeslot ----- //

public class Evaluator
{
	
	// ----- Global Variables ----- //
	
	LinkedList<CoursePair> course_pairs;
	LinkedList<TimeCoursePair> time_course_pairs;
	int pen_coursemin;
	int pen_labsmin;
	int pen_notpaired;
	int pen_section;
	int weight_minfilled;
	int weight_pref;
	int weight_pair;
	int weight_secdiff;
	boolean enable_debug_mode = false;
	
	// ----- Constructors ----- //
	
	// Description: Constructor that sets up the weights, penalties, and other values
	public Evaluator(LinkedList<CoursePair> inCoursePairs, LinkedList<TimeCoursePair> inTimeCoursePairs, 
	int inPenCourseMin, int inPenLabsMin, int inPenNotPaired, int inPenSection, int inWeightMinFilled, int inWeightPref, int inWeightPair, int inWeightSecDiff)
	{
		setCoursePairs(inCoursePairs); setTimeCoursePairs(inTimeCoursePairs); setPenCourseMin(inPenCourseMin); setPenLabsMin(inPenLabsMin); setPenNotPaired(inPenNotPaired);
		setPenSection(inPenSection); setWeightMinFilled(inWeightMinFilled); setWeightPref(inWeightPref); setWeightPair(inWeightPair); setWeightSecDiff(inWeightSecDiff);
	}
	// Description: Constructor used in absence of weight values
	public Evaluator(LinkedList<CoursePair> inCoursePairs, LinkedList<TimeCoursePair> inTimeCoursePairs, int inPenCourseMin, int inPenLabsMin, int inPenNotPaired, int inPenSection)
	{
		setCoursePairs(inCoursePairs); setTimeCoursePairs(inTimeCoursePairs); setPenCourseMin(inPenCourseMin); setPenLabsMin(inPenLabsMin); setPenNotPaired(inPenNotPaired);
		setPenSection(inPenSection); setWeightMinFilled(1); setWeightPref(1); setWeightPair(1); setWeightSecDiff(1);
	}

	//Eval data is a static class where all eval data can be obtained and placed in where needed.
	//Added this constructor so that the number of parameters were not so crazy
	public Evaluator(FileData inFileData){
		course_pairs = (LinkedList<CoursePair>)inFileData.pair.clone();
		time_course_pairs = inFileData.getPreferences();
		weight_minfilled = EvalData.getWminfilled();
		weight_pref = EvalData.getWpref();
		weight_pair = EvalData.getWpair();
		weight_secdiff = EvalData.getWsecdiff();
		pen_coursemin = EvalData.getPen_coursemin();
		pen_labsmin = EvalData.getPen_labsmin();
		pen_notpaired = EvalData.getPen_notpaired();
		pen_section = EvalData.getPen_section();
	}

	// ----- Evaluators ----- //
	
	/* Description: Evaluates a given list of Timeslots with an integer based on satisfacton of the soft constraints
	> inTimeSlots : the set of timeslots to be evaluated
	> Returns : An Integer Value Representing soft constraints not met
	*/ 
	public int evaluateTimeslots(LinkedList<Timeslot> inTimeSlots)
	{
		enable_debug_mode = false;
		if(enable_debug_mode) {
			for (Timeslot ts : inTimeSlots){
				//System.out.println(ts.getLocalSlot().getDay() + " " + ts.getLocalSlot().getStartTime() + " Contains: ");
				for (courseItem c : ts.getAssignedItems()) {
					//System.out.println(c.getNumber() + " " + c.getSection() + c.getTutVLab());
				}
			}
		}
		return (getWeightMinFilled() * EvalMinFilled(inTimeSlots)) + (getWeightPref() * EvalPref(inTimeSlots)) + (getWeightPair() * EvalPair(inTimeSlots)) + (getWeightSecDiff() * EvalSecDiff(inTimeSlots));
	}
	
	// Evaluates a given list of Timeslots with an integer based on how many courses/labs meet the minimum requirement assignments for a single timeslot
	public int EvalMinFilled(LinkedList<Timeslot> inTimeSlots)
	{
		int result = 0;
		for (Timeslot aTimeSlot : inTimeSlots)
		{
			int course_count = 0;
			int lab_count = 0;
			for (courseItem item : aTimeSlot.getAssignedItems())
			{
				/*
				if (Arrays.stream(DataParser.validLecType).anyMatch(item.getLecVsTut()::equals)) course_count++;
				else if (Arrays.stream(DataParser.validTutType).anyMatch(item.getLecVsTut()::equals)) lab_count++;
				*/
				if (item.isALec) course_count++;
				else lab_count++;
			}
			if(enable_debug_mode) System.out.println(("Timeslot contains " + course_count + " courses and, " + lab_count + " labs"));
			if ((!aTimeSlot.forCourses) && (aTimeSlot.getLocalSlot().getMin() > lab_count)) {result = (result + (getPenLabsMin()) * (aTimeSlot.getLocalSlot().getMin() - lab_count));
				//if(enable_debug_mode) System.out.println(("Minimum amount of courses for this Timeslot: " + aTimeSlot.getLocalSlot().getMin() + " and there are " + lab_count + " labs, and penalty multiplier is: " + getPenLabsMin()));
				//if(enable_debug_mode) System.out.println(("Adding: " + (getPenLabsMin()) * (aTimeSlot.getLocalSlot().getMin() - lab_count)));
			}
			else if ((aTimeSlot.forCourses) && (aTimeSlot.getLocalSlot().getMin() > course_count)) { result = (result + (getPenCourseMin()) * (aTimeSlot.getLocalSlot().getMin() - course_count));
				//if(enable_debug_mode) System.out.println(("Minmum amount of courses for this Timeslot is: " + aTimeSlot.getLocalSlot().getMin() + " and there are " + course_count + " courses, and penalty multiplier is: " + getPenCourseMin()));
				//if(enable_debug_mode) System.out.println(("Adding: " + (getPenCourseMin()) * (aTimeSlot.getLocalSlot().getMin() - course_count)));
			}
		}
		return result;
	}
	
	/* Description: Evaluates a given list of Timeslots with an integer based on how many prefered assignments of courses to TimeSlots are met
	> inTimeSlots : the set of timeslots to be evaluated
	> Returns : An Integer Value Representing prefered assignments of courses not met
	*/
	public int EvalPref(LinkedList<Timeslot> inTimeSlots)
	{
		int result = 0;
		
		for (TimeCoursePair tcp : getTimeCoursePairs())
		{
			boolean not_found = true;
			//("Course is: " + tcp.getCourseItem().getNumber() + tcp.getCourseItem().getSection() + tcp.getCourseItem().getTutVLab() + " Timeslot is: " + tcp.getTime().getDay() + tcp.getTime().getStartTime());
			//int counter = 0;
			for (Timeslot ts : inTimeSlots){
				if (ts.getLocalSlot().isSameSlot(tcp.getTime()) && ((ts.forCourses && tcp.getCourseItem().isALec)||(!ts.forCourses && !tcp.getCourseItem().isALec)))
				{
					//System.out.print(counter);
					if (tcp.getCourseItem().isALec) {
						//System.out.print("this is a lecture");
					}
					not_found = false;
					//("got here" + ts.getLocalSlot().getDay() + ts.getLocalSlot().getStartTime());
					if (!existsInTimeslot(tcp.getCourseItem(), ts))
					{
						if(enable_debug_mode) System.out.println((tcp.getCourseItem().getNumber() + tcp.getCourseItem().getLecVsTut() + tcp.getCourseItem().getSection() + " doesnt exist: in: " + tcp.getTime().getDay() + " " + tcp.getTime().getStartTime() + " adding penalty : " + tcp.prefVal));
						result = result + tcp.prefVal;
					}
					continue;
				}	
				//counter++;
			}
			if (not_found) {
				if(enable_debug_mode) System.out.println(("Not found: " + tcp.getCourseItem().getNumber() + tcp.getCourseItem().getSection() + tcp.getCourseItem().getLecVsTut() + " doesnt exist: in: " + tcp.getTime().getDay() + " " + tcp.getTime().getStartTime() + " adding penalty : " + tcp.prefVal));
				result = result + tcp.prefVal;
			}
			
		}
		return result;
	}
	
	/* Description: Evaluates a given list of Timeslots with an integer based on how many prefered pairings of courses are met
	> inTimeSlots : the set of timeslots to be evaluated
	> Returns : An Integer Value Representing penalties for how many prefered pairings of courses are not met
	*/
	public int EvalPair(LinkedList<Timeslot> inTimeSlots)
	{
		int result = 0;
		for (CoursePair aCoursePair : getCoursePairs())
		{
			for (Timeslot aTimeSlot : inTimeSlots)
			{
				if(enable_debug_mode) System.out.println(("Checking course pairs: " + aCoursePair.getItemOne().getNumber() + " " + aCoursePair.getItemTwo().getNumber() + "in timeslot : " + aTimeSlot.getLocalSlot().getDay() + aTimeSlot.getLocalSlot().getStartTime()));
				if ((!existsInTimeslot(aCoursePair.getItemOne(), aTimeSlot) & existsInTimeslot(aCoursePair.getItemTwo(), aTimeSlot)) || (existsInTimeslot(aCoursePair.getItemOne(), aTimeSlot) & !existsInTimeslot(aCoursePair.getItemTwo(), aTimeSlot))) {
					//(aCoursePair.getItemOne().getNumber() + "is not paired with" + aCoursePair.getItemTwo().getNumber() + "adding penalty : " + getPenNotPaired());
					result = result + getPenNotPaired(); 
					break;
				}
			}
		}
		return result;
	}
	
	/* Description: Evaluates a given list of Timeslots with an integer based on how many different sections of the same courses are assigned the same Timeslot
	> inTimeSlots : the set of timeslots to be evaluated
	> Returns : An Integer Value Representing penalities for how many different sections of the same courses are not assigned the same Timeslot
	*/
	public int EvalSecDiff(LinkedList<Timeslot> inTimeSlots)
	{
		int result = 0;
		for (Timeslot aTimeSlot : inTimeSlots)
		{
			result = result + (getSectionPairs(aTimeSlot).size() * getPenSection());
		}
		return result;
	}
	
	// ----- Other Methods ----- //
	
	/* Description: Creates a list of pairs of courses that are of the same section
	> inTimeSlots : a timeslot to be searched
	> Returns : A linked list of CoursePairs of which are the same course but different section numbers
	*/
	public LinkedList<CoursePair> getSectionPairs(Timeslot inTimeSlot)
	{
		LinkedList<CoursePair> sec_pairs = new LinkedList<CoursePair>();
		for (int i = 0; i < (inTimeSlot.getAssignedItems().size() - 1); i++)
		{	
			//(inTimeSlot.getAssignedItems().get(i).getNumber() + " " + inTimeSlot.getAssignedItems().get(i).getSection() + inTimeSlot.getAssignedItems().get(i).getTutVLab());
			for (int j =  i + 1; j < inTimeSlot.getAssignedItems().size(); j++)
			{
				//("item j is: " + inTimeSlot.getAssignedItems().get(j).getNumber() + " " + inTimeSlot.getAssignedItems().get(j).getSection() + inTimeSlot.getAssignedItems().get(j).getTutVLab());
				//("j is: " + j);
				if (isSameCourseDifferentSection(inTimeSlot.getAssignedItems().get(i), inTimeSlot.getAssignedItems().get(j)))
				{
					//("got here");
					sec_pairs.add(new CoursePair(inTimeSlot.getAssignedItems().get(i), inTimeSlot.getAssignedItems().get(j)));
				}
			}
		}
		//("Eval for Sec pair size is: " + sec_pairs.size());
		return sec_pairs;
	}
	
	/* Description: Checks if a given courses exists in a given Timeslot
	> inItem : an item to be search for 
	> inTimeSlots : a timeslot to be searched
	> Returns : True if the item 'inItem' exists in the Timeslot 'inTimeSlots' and False if it does not exist in the Timeslot 'inTimeSlots'
	*/
	public Boolean existsInTimeslot(courseItem inItem, Timeslot inTimeSlot)
	{
		for (courseItem aItem : inTimeSlot.getAssignedItems())
		{
			if (inItem.isSameCourseItems(aItem)) return true;
		}
		return false;
	}
	
	/* Description: Checks if two courses are the same but are different sections
	> inItem1 : a courseItem to be compaired
	> inItem2 : a courseItem to be compaired
	> Returns : True if 'inItem1' and 'inItem2' are the same course but different sections
	*/
	public Boolean isSameCourseDifferentSection(courseItem inItem1, courseItem inItem2)
	{
		if(!inItem1.getDepartment().equals(inItem2.getDepartment())) {
			//("got here 1" + inItem1.getDepartment() + inItem2.getDepartment());
			return false;
		}
		if(!inItem1.getNumber().equals(inItem2.getNumber())) {
			//("got here 1" + inItem1.getNumber() + inItem2.getNumber());
			return false;
		}
		if(!inItem1.getLecVsTut().equals(inItem2.getLecVsTut())) {
			//("got here 1" + inItem1.getLecVsTut() + inItem2.getLecVsTut());
			return false;
		}
		if(inItem1.getSection().equals(inItem2.getSection())) {
			//("got here 1" + inItem1.getSection() + inItem2.getSection());
			return false;
		}
		if(!inItem1.getTutVLab().equals(inItem2.getTutVLab())) {
			//("got here 1" + inItem1.getTutVLab() + inItem2.getTutVLab());
			return false;
		}
		if(!inItem1.getTutSection().equals(inItem2.getTutSection())) {
			//("got here 1" + inItem1.getTutSection() + inItem2.getTutSection());
			return false;
		}
		return true;
	}
	
	// ---------- Getters  ----------- //

	public LinkedList<CoursePair> getCoursePairs()
	{
		return this.course_pairs;
	}
	public LinkedList<TimeCoursePair> getTimeCoursePairs()
	{
		return this.time_course_pairs;
	}
	public int getPenCourseMin()
	{
		return this.pen_coursemin;
	}
	public int getPenLabsMin()
	{
		return this.pen_labsmin;
	}
	public int getPenSection()
	{
		return this.pen_section;
	}
	public int getPenNotPaired()
	{
		return this.pen_notpaired;
	}
	public int getWeightMinFilled()
	{
		return this.weight_minfilled;
	}
	public int getWeightPair()
	{
		return this.weight_pair;
	}
	public int getWeightPref()
	{
		return this.weight_pref;
	}
	public int getWeightSecDiff()
	{
		return this.weight_secdiff;
	}
	
	// ---------- Setters  ----------- //
	
	public void setCoursePairs(LinkedList<CoursePair> inCoursePairs)
	{
		this.course_pairs = inCoursePairs;
	}
	public void setTimeCoursePairs(LinkedList<TimeCoursePair> inTimeCoursePairs)
	{
		this.time_course_pairs = inTimeCoursePairs;
	}
	public void setPenCourseMin(int inPenCourseMin)
	{
		this.pen_coursemin = inPenCourseMin;
	}
	public void setPenLabsMin(int inPenLabsMin)
	{
		this.pen_labsmin = inPenLabsMin;
	}
	public void setPenNotPaired(int inPenNotPaired)
	{
		this.pen_notpaired = inPenNotPaired;
	}
	public void setPenSection(int inPenSection)
	{
		this.pen_section = inPenSection;
	}
	public void setWeightMinFilled(int inWeightMinFilled)
	{
		this.weight_minfilled = inWeightMinFilled;
	}
	public void setWeightPair(int inWeightPair)
	{
		this.weight_pair = inWeightPair;
	}
	public void setWeightPref(int inWeightPref)
	{
		this.weight_pref = inWeightPref;
	}
	public void setWeightSecDiff(int inWeightSecDiff)
	{
		this.weight_secdiff = inWeightSecDiff;
	}
}