import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class CourseAdapter(
    private var courses: List<Pair<String, String>>,
    private val onLongClick: (Int) -> Unit,
    private val onItemClick: (Pair<String, String>) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.textCourseName)
        val subject: TextView = itemView.findViewById(R.id.textsubject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.`jk_course_item`, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val (subject, course) = courses[position]
        holder.courseName.text = course
        holder.subject.text = subject

        holder.itemView.setOnLongClickListener {
            onLongClick(position)
            true
        }

        holder.itemView.setOnClickListener {
            onItemClick(courses[position])
        }
    }

    override fun getItemCount(): Int = courses.size

    fun updateData(newCourses: List<Pair<String, String>>) {
        courses = newCourses
        notifyDataSetChanged()
    }
}
