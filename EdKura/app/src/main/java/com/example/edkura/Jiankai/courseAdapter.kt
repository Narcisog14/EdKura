<<<<<<< HEAD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class CourseAdapter(
    private var courses: List<Pair<String, String>>,
    private val onLongClick: (Int) -> Unit,
    private val onItemClick: (Pair<String, String>) -> Unit // ✅ 新增点击事件回调
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

        // ✅ 添加点击事件
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
=======
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class CourseAdapter(
    private var courses: List<Pair<String, String>>,
    private val onLongClick: (Int) -> Unit,
    private val onItemClick: (Pair<String, String>) -> Unit // ✅ 新增点击事件回调
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.textCourseName)
        val subject: TextView = itemView.findViewById(R.id.textSubject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.course_item, parent, false)
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

        // ✅ 添加点击事件
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
>>>>>>> a22fca2c470fe485af5c7aa1a81ab0ca7753fcbf
